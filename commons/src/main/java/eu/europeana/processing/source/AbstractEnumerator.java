package eu.europeana.processing.source;

import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.exception.FlinkWorkflowException;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.AbstractPartition;
import eu.europeana.processing.repository.TaskInfoRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import lombok.Getter;
import org.apache.flink.api.connector.source.SourceEvent;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.util.ParameterTool;
import org.apache.flink.runtime.execution.SuppressRestartsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base Flink enumerator base for different sources, implementing checkpointing failover.
 *
 * @param <P> split type that is used by the source implementation
 * @param <S> state used by an implementation of enumerator which is saved in snapshot
 */
public abstract class AbstractEnumerator<P extends AbstractPartition, S extends AbstractEnumeratorState<P>> implements
    SplitEnumerator<P, S> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEnumerator.class);
  private static final int DEFAULT_CHUNK_SIZE = 1000;
  public static final int NOT_EVALUATED = -1;

  protected final SplitEnumeratorContext<P> context;
  protected final ParameterTool parameterTool;
  protected final int chunkSize;
  protected final long taskId;

  @Getter
  protected final UUID enumeratorId = UUID.randomUUID();


  TaskInfoRepository taskInfoRepo;

  protected long startedRecordsCount = 0;
  protected long emittedRecordCount = 0;
  protected ProgressUpdater progressUpdater;
  protected final Map<String, P> returnedPartitions = new LinkedHashMap<>();
  protected final Map<String, P> executingPartitions = new LinkedHashMap<>();
  protected long recordsToBeProcessed = NOT_EVALUATED;
  protected Queue<Integer> waitingReaders = new LinkedList<>();

  /**
   * Constructor used when state restoration is needed;
   *
   * @param context context for enumerator
   * @param parameterTool parameter tool
   */
  protected AbstractEnumerator(SplitEnumeratorContext<P> context, ParameterTool parameterTool) {
    this.context = context;
    this.parameterTool = parameterTool;
    this.taskId = parameterTool.getLong(JobParamName.TASK_ID);
    this.chunkSize = parameterTool.getInt(JobParamName.CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
  }

  /**
   * Constructor used when state restoration is needed;
   *
   * @param context context for enumerator
   * @param state enumerator state container
   * @param parameterTool parameter tool
   */
  protected AbstractEnumerator(SplitEnumeratorContext<P> context, ParameterTool parameterTool, S state) {
    this(context, parameterTool);
    recordsToBeProcessed = state.getRecordsToBeProcessed();
    startedRecordsCount = state.getStartedRecordsCount();
    emittedRecordCount = state.getFinishedRecordCount();
    for (P split : state.getIncompletePartitions()) {
      returnedPartitions.put(split.splitId(), (P) split.withEnumeratorId(enumeratorId));
    }
  }

  @Override
  public void start() {
    LOGGER.info("Starting enumerator");
    taskInfoRepo = RetryableMethodExecutor.createRetryProxy(new TaskInfoRepository(new DbConnectionProvider(parameterTool)));
    progressUpdater = new ProgressUpdater(
        taskInfoRepo,
        parameterTool,
        emittedRecordCount);
    createDbRepositories();
    try {
      validateTaskExists();
    } catch (FlinkWorkflowException e) {
      throw new SuppressRestartsException(e);
    }
  }

  protected abstract void createDbRepositories();

  @Override
  public void handleSplitRequest(int subtaskId, String requesterHostname) {
    P splitToBeServed;
    if (!returnedPartitions.isEmpty()) {
      splitToBeServed = returnedPartitions.remove(returnedPartitions.keySet().iterator().next());
    } else if ((splitToBeServed = createNextPartition()) == null) {
      handleNoPartitionsAvailable(subtaskId);
      return;
    }
    executingPartitions.put(splitToBeServed.splitId(), splitToBeServed);
    context.assignSplit(splitToBeServed, subtaskId);
    LOGGER.info("Assigned split: {} for subtaskId: {}, host: {}. Executing: {} of: {} started records, finished: {}",
        splitToBeServed, subtaskId, requesterHostname, executingPartitions.size(), startedRecordsCount,
        emittedRecordCount);
  }

  protected void handleNoPartitionsAvailable(int subtaskId) {
    if (isFinished()) {
      notifyNoMoreSplits(subtaskId);
    } else {
      waitingReaders.add(subtaskId);
      LOGGER.info("No more splits currently available for subtask: {}, waiting readers: {}!", subtaskId, waitingReaders);
    }
  }


  protected void notifyNoMoreSplits(int subtaskId) {
    LOGGER.info("No more remaining splits, currently executing {} splits!", executingPartitions.size());
    context.signalNoMoreSplits(subtaskId);
  }

  protected abstract P createNextPartition();

  @Override
  public void addSplitsBack(List<P> splits, int subtaskId) {
    for (P split : splits) {
      addSplitBack(split, subtaskId);
    }

  }

  private void addSplitBack(P split, int subtaskId) {
    P inExecutingMap = executingPartitions.remove(split.splitId());
    if (inExecutingMap != null) {
      split = updateProgress(inExecutingMap, split.getProgress());
    }
    returnedPartitions.put(split.splitId(), updateProgress(returnedPartitions.get(split.splitId()), split.getProgress()));
    LOGGER.info(
        "Added split: {} from subtask: {} back. Currently executing: {} splits, all returned splits: {}",
        split, subtaskId, executingPartitions.size(), returnedPartitions.size());
  }

  private P updateProgress(P previousSplit, long progress) {
    LOGGER.debug("Updating progress to: {}, for split: {}", progress, previousSplit);
    P current = (P) previousSplit.withProgress(progress);
    long progressIncrease = current.getProgress() - previousSplit.getProgress();
    emittedRecordCount += progressIncrease;
    return current;
  }

  private void updateProgressOfExecutingSplit(String splitId, long progress) {
    executingPartitions.put(splitId, updateProgress(executingPartitions.get(splitId), progress));
  }

  @Override
  public void handleSourceEvent(int subtaskId, SourceEvent sourceEvent) {
    if (sourceEvent instanceof ProgressSnapshotEvent event) {
      handleProgressSnapshotEvent(event);
    } else if (sourceEvent instanceof SplitCompletedEvent event) {
      handleSplitCompletedEvent(event);
    }
  }

  private void handleProgressSnapshotEvent(ProgressSnapshotEvent event) {
    if (!event.getEnumeratorId().equals(enumeratorId)) {
      LOGGER.info("Enumerator: {}, received a ProgressSnapshotEvent from reader from different attempt: {}", enumeratorId, event);
      return;
    }

    executingPartitions.put(event.getSplitId(),
        updateProgress(executingPartitions.get(event.getSplitId()), event.getProgress()));
    LOGGER.debug("Received progress information: {}", event);
  }

  private void handleSplitCompletedEvent(SplitCompletedEvent event) {
    if (!event.getEnumeratorId().equals(enumeratorId)) {
      LOGGER.info("Enumerator: {}, received a SplitCompletedEvent from reader from different attempt: {}", enumeratorId, event);
      return;
    }

    updateProgressOfExecutingSplit(event.getSplitId(), event.getCompletedCount());
    //We always remove it even, if not all records were performed because of some bugs.
    executingPartitions.remove(event.getSplitId());
    LOGGER.info("Split completed: {}. Now executing {} splits. Finished {} of: {} records.",
        event, executingPartitions.size(), emittedRecordCount, recordsToBeProcessed);
    if (isFinished()) {
      tryAssignWaitingReaders();
    }
  }

  @Override
  public void addReader(int subtaskId) {
    LOGGER.info("New reader added for, the subtaskId: {}", subtaskId);
  }

  @Override
  public S snapshotState(long checkpointId) {
    S state = createState();
    state.setStartedRecordsCount(startedRecordsCount);
    state.setFinishedRecordCount(emittedRecordCount);
    state.setIncompletePartitions(getIncompletePartitionsSnapshot());
    progressUpdater.snapshotEmittedFilesCount(emittedRecordCount);
    LOGGER.info("Creating snapshot of state for the checkpoint: {}, state: {}", checkpointId, state);
    return state;
  }

  protected abstract S createState();

  @Override
  public void notifyCheckpointComplete(long checkpointId) throws FlinkWorkflowException {
    LOGGER.debug("Task: {}, checkpoint: {} completed. Updating progress...", taskId, checkpointId);
    progressUpdater.saveProgressInDB();
  }


  @Override
  public void notifyCheckpointAborted(long checkpointId) {
    LOGGER.info("Checkpoint aborted: {}!", checkpointId);
  }

  @Override
  public void close() throws IOException {
    LOGGER.info("Closing enumerator.");
    taskInfoRepo.shutdown();
  }


  private List<P> getIncompletePartitionsSnapshot() {
    List<P> incompletePartitions = new ArrayList<>();
    incompletePartitions.addAll(executingPartitions.values());
    incompletePartitions.addAll(returnedPartitions.values());
    return incompletePartitions;
  }

  private void validateTaskExists() throws FlinkWorkflowException {
    try {
      if (taskInfoRepo.findById(taskId).isEmpty()) {
        throw new SuppressRestartsException(new Exception("Task not found in the database. It should never happen."));
      }
    } catch (FlinkWorkflowException e){
      throw e;
    }
  }

  protected boolean isFinished() {
    return executingPartitions.isEmpty() && returnedPartitions.isEmpty();
  }

  protected void tryAssignWaitingReaders() {
    Queue<Integer> readyReaders = waitingReaders;
    waitingReaders = new LinkedList<>();
    for (int reader : readyReaders) {
      handleSplitRequest(reader, "");
    }
  }

}
