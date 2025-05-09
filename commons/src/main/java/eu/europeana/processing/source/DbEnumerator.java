package eu.europeana.processing.source;

import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.DataPartition;
import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.repository.TaskInfoRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import eu.europeana.processing.source.DbEnumeratorState.DbEnumeratorStateBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.flink.api.connector.source.SourceEvent;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.util.ParameterTool;
import org.apache.flink.runtime.execution.SuppressRestartsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Flink enumerator that provides splits for Metis jobs using as a source PostgresDB
 * @param <S> state used by an implementation of enumerator which is saved in snapshot
 * @param <B> state builder for enumerator.
 */
public abstract class DbEnumerator<S extends DbEnumeratorState,B extends DbEnumeratorStateBuilder<S,?>> implements SplitEnumerator<DataPartition, S> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbEnumerator.class);
  private static final int DEFAULT_CHUNK_SIZE = 1000;
  public static final int NOT_EVALUATED = -1;

  protected final SplitEnumeratorContext<DataPartition> context;
  protected final ParameterTool parameterTool;
  private final int chunkSize;
  private final long taskId;

  TaskInfoRepository taskInfoRepo;
  protected DbConnectionProvider dbConnectionProvider;

  protected long recordsToBeProcessed;
  private long startedRecordsCount;
  protected long finishedRecordCount;
  private final NavigableMap<Long, Long> checkpointIdToFinishedRecordCountMap = new TreeMap<>();
  private long commitCount;
  private List<DataPartition> returnedPartitions;
  private final Map<DataPartition, SplitProgressInfo> executingPartitions = new LinkedHashMap<>();

  /**
   *
   * Constructor used when state restoration is not needed;
   *
   * @param context context for enumerator
   * @param parameterTool parameter tool
   */
  protected DbEnumerator(SplitEnumeratorContext<DataPartition> context,
      ParameterTool parameterTool) {
    this(context, null, parameterTool);
  }

  /**
   * Constructor used when state restoration is needed;
   *
   * @param context context for enumerator
   * @param state enumerator state container
   * @param parameterTool parameter tool
   */
  protected DbEnumerator(SplitEnumeratorContext<DataPartition> context, S state,
      ParameterTool parameterTool) {
    this.context = context;
    this.parameterTool = parameterTool;
    this.taskId = parameterTool.getLong(JobParamName.TASK_ID);
    this.chunkSize = parameterTool.getInt(JobParamName.CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
    if (state != null) {
      restoreEnumeratorFromState(state);
    } else {
      initEnumerator();
    }
  }

  private void initEnumerator() {
    recordsToBeProcessed = NOT_EVALUATED;
    startedRecordsCount = 0;
    finishedRecordCount = 0;
    commitCount = 0;
    returnedPartitions = new ArrayList<>();
    LOGGER.info("Created DbEnumerator with no fetched partitions");
  }

  private void restoreEnumeratorFromState(DbEnumeratorState state) {
    recordsToBeProcessed = state.getRecordsToBeProcessed();
    startedRecordsCount = state.getStartedRecordsCount();
    finishedRecordCount = state.getFinishedRecordCount();
    commitCount = state.getCommitCount();
    returnedPartitions = state.getIncompletePartitions();
    LOGGER.info(
        "Restored DbEnumerator with finished: {} of: {} started, of {} records to be processed. Returned: {} partitions: {}",
        finishedRecordCount,startedRecordsCount, recordsToBeProcessed , returnedPartitions.size(),   returnedPartitions);
  }

  @Override
  public void start() {
    LOGGER.info("Starting DbEnumerator");
    dbConnectionProvider = new DbConnectionProvider(parameterTool);
    createDbRepositories();
    taskInfoRepo = RetryableMethodExecutor.createRetryProxy(new TaskInfoRepository(dbConnectionProvider));
    validateTaskExists();
    if (recordsToBeProcessed == NOT_EVALUATED) {
      evaluateRecordsCount();
    } else {
      LOGGER.info("Record count already evaluated. Finished: {} of {} all records.",
          finishedRecordCount, recordsToBeProcessed);
    }
  }

  protected abstract void createDbRepositories();

  @Override
  public void handleSplitRequest(int subtaskId, String requesterHostname) {
    DataPartition splitToBeServed;
    if (!returnedPartitions.isEmpty()) {
      splitToBeServed = returnedPartitions.removeFirst();
    } else if (startedRecordsCount < recordsToBeProcessed) {
      splitToBeServed = createNextPartition();
    } else {
      handleNoPartitionsAvailable(subtaskId);
      return;
    }
    executingPartitions.put(splitToBeServed, new SplitProgressInfo());
    context.assignSplit(splitToBeServed, subtaskId);
    LOGGER.info("Assigned split: {} for subtaskId: {}, host: {}. Executing: {} of: {} started records, finished: {}",
        splitToBeServed, subtaskId, requesterHostname, executingPartitions.size(), startedRecordsCount,
        finishedRecordCount);
  }

  protected void handleNoPartitionsAvailable(int subtaskId) {
    LOGGER.info("No more remaining splits, currently executing {} splits!", executingPartitions.size());
    context.signalNoMoreSplits(subtaskId);
  }

  private DataPartition createNextPartition() {
    //TODO size of the split should be adjusted to parallelization level to work optimal
    //Is good to do the adjustment in some place.
    long partitionSize = Long.min(recordsToBeProcessed - startedRecordsCount, chunkSize);
    DataPartition partition = new DataPartition(startedRecordsCount, partitionSize);
    startedRecordsCount += partitionSize;
    return partition;
  }

  @Override
  public void addSplitsBack(List<DataPartition> splits, int subtaskId) {
    for (DataPartition split : splits) {
      addSplitBack(split, subtaskId);
    }

  }

  private void addSplitBack(DataPartition split, int subtaskId) {
    SplitProgressInfo info = removeSplitFromExecutingMap(split);
    DataPartition updatedSplit = createSplitWithoutCompletedRecords(split, info);
    if (split.limit() > 0) {
      returnedPartitions.add(updatedSplit);
      LOGGER.info(
          "Added split: {} from subtask: {} back. Updated split: {}. Currently executing: {} splits, all returned splits: {}",
          split, subtaskId, updatedSplit, executingPartitions.size(), returnedPartitions.size());
    } else {
      LOGGER.warn("Added split: {} from subtask: {} back, but the subtask is already completed! Info: {}."
              + "Currently executing: {} splits, all returned splits: {}",
          subtaskId, split, info, executingPartitions.size(), returnedPartitions.size());

    }
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
    SplitProgressInfo progressInfo = getSplitFromExecutingMap(event);
    finishedRecordCount += progressInfo.update(event);
    LOGGER.info("Received progress information: {}", event);
  }

  private void handleSplitCompletedEvent(SplitCompletedEvent event) {
    DataPartition split = event.getSplit();
    SplitProgressInfo info = removeSplitFromExecutingMap(split);
    finishedRecordCount += info.update(event);
    LOGGER.info("Split completed: {}. Now executing {} splits. Finished {} of: {} records.",
        event, executingPartitions.size(), finishedRecordCount, recordsToBeProcessed);
  }

  @Override
  public void addReader(int subtaskId) {
    LOGGER.info("New reader added for, the subtaskId: {}", subtaskId);
  }

  @Override
  public S snapshotState(long checkpointId) {
    S state = createSnapshotBuilder()
        .recordsToBeProcessed(recordsToBeProcessed)
        .startedRecordsCount(startedRecordsCount)
        .finishedRecordCount(finishedRecordCount)
        .commitCount(commitCount)
        .incompletePartitions(getIncompletePartitionsSnapshot())
        .build();
    checkpointIdToFinishedRecordCountMap.put(checkpointId, finishedRecordCount);
    LOGGER.info("Creating snapshot of state for the checkpoint: {}, state: {}", checkpointId, state);
    return state;
  }

  protected abstract B createSnapshotBuilder();


  @Override
  public void notifyCheckpointComplete(long checkpointId) {
    LOGGER.info("Checkpoint: {} completed. Updating progress... Map:{}", checkpointId, checkpointIdToFinishedRecordCountMap);
    SortedMap<Long, Long> approvedProgresses = checkpointIdToFinishedRecordCountMap.headMap(checkpointId, true);
    Entry<Long, Long> lastApprovedProgress = approvedProgresses.lastEntry();
    if (lastApprovedProgress != null) {
      //TODO Commit count is not strictly evaluated cause in case of restart of job in case of exception
      //This value is lost and set to the last snapshot value. So we need to increase it earlier
      // during snapshot start and store here but we also need not increase it again if snapshot
      // is aborted, so it is a bit difficult.
      //We could consider small optimisation to not save progress if there is no change in it.
      //Then for example we could save modification date to db, for debug purpose.
      TaskInfo taskInfo = new TaskInfo(taskId, ++commitCount, lastApprovedProgress.getValue());

      //TODO The repository uses retries in case of failure, but because updating progress is not a key feature,
      // without it the task should finish its work properly. Beside that we could omit some updates of progress
      // as long as we store last progress, when the task is whole complete.
      // So we could consider more sophisticated failover mechanism with lesser impact on the execution.
      taskInfoRepo.update(taskInfo);

      approvedProgresses.clear();
      LOGGER.info("Updated task progress in DB: {}", taskInfo);
    } else {
      LOGGER.info("There is not approved progress to update in DB for checkpoint id: {}. Progress map: {}",
          checkpointId, checkpointIdToFinishedRecordCountMap);
    }

  }

  @Override
  public void notifyCheckpointAborted(long checkpointId) {
    LOGGER.info("Checkpoint aborted: {}!", checkpointId);
  }

  @Override
  public void close() throws IOException {
    if (dbConnectionProvider != null) {
      dbConnectionProvider.close();
    }
  }

  private void evaluateRecordsCount() {
    LOGGER.info("Preparing split count...");
    try {
      recordsToBeProcessed = countRecordsInDb();
      LOGGER.info("Finished, there is: {} records to be processed!", recordsToBeProcessed);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract long countRecordsInDb() throws IOException;

  private List<DataPartition> getIncompletePartitionsSnapshot() {
    List<DataPartition> incompletePartitions = new ArrayList<>();
    for (Entry<DataPartition, SplitProgressInfo> entry : executingPartitions.entrySet()) {
      DataPartition split = createSplitWithoutCompletedRecords(entry.getKey(), entry.getValue());
      if (split.limit() > 0) {
        incompletePartitions.add(split);
      }
    }
    incompletePartitions.addAll(returnedPartitions);
    return incompletePartitions;
  }

  private void validateTaskExists() {
    if (taskInfoRepo.findById(taskId).isEmpty()) {
      throw new SuppressRestartsException(new Exception("Task not found in the database. It should never happen."));
    }
  }

  private SplitProgressInfo getSplitFromExecutingMap(ProgressSnapshotEvent event) {
    DataPartition split = event.getSplit();
    SplitProgressInfo progressInfo = executingPartitions.get(split);
    if (progressInfo == null) {
      throw new SourceConsistencyException("Could not find the split in the executing map, for received progress event: "
          + event + ". Presence of split in the returned list: " + returnedPartitions.contains(split));
    }
    return progressInfo;
  }

  private SplitProgressInfo removeSplitFromExecutingMap(DataPartition split) {
    SplitProgressInfo info = executingPartitions.remove(split);
    if (info == null) {
      //TODO Check if it could happen anyway. Maybe we coudl ignore it, cause split is already completed
      throw new SourceConsistencyException("Could not find split: " + split + " in the executed splits!");
    }
    return info;
  }

  /**
   * Method trim partition to not contain already completed records. it is needed in case when we need to retry given partition
   * for example after job restarting. The result of this method could be split with limit 0 in rare cases.
   *
   * @param split - original split
   * @param info - info about progress
   * @return new trimmed split.
   */
  private static DataPartition createSplitWithoutCompletedRecords(DataPartition split, SplitProgressInfo info) {
    return new DataPartition(split.offset() + info.getEmittedRecordCount(), split.limit() - info.getEmittedRecordCount());
  }

}
