package eu.europeana.processing.source;

import eu.europeana.processing.exception.FlinkWorkflowException;
import eu.europeana.processing.job.JobParam;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.DataPartition;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.util.ParameterTool;
import org.apache.flink.core.io.InputStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Base class for readers emitting records read directly from DB based on some query constraint defined in split (partition).
 * @param <R> – The type of the record emitted by this source reader.
 */
public abstract class AbstractDbReader<R> implements SourceReader<R, DataPartition> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDbReader.class);

    private static final long INITIAL_CHECKPOINT_ID = -1;
    private final SourceReaderContext context;
    protected final ParameterTool parameterTool;

    private CompletableFuture<Void> readerAvailable = new CompletableFuture<>();
    private final int maxRecordPending;
    private int currentRecordPendingCount;
    private int allCommittedRecordCount;
    private int currentSplitCommittedRecordCount;
    private final TreeMap<Long, Integer> recordPendingCountPerCheckpoint = new TreeMap<>();
    private boolean splitFetched = false;
    private boolean noMoreSplits = false;
    private long currentCheckpointId = INITIAL_CHECKPOINT_ID;

    private List<R> polledRecords = null;

    private List<DataPartition> currentSplits = new ArrayList<>();
    protected DataPartition currentSplit;

    protected AbstractDbReader(
            SourceReaderContext context,
            ParameterTool parameterTool) {
        this.context = context;
        this.parameterTool = parameterTool;
        maxRecordPending = parameterTool.
                getInt(
                        JobParamName.MAX_RECORD_PENDING,
                        JobParam.DEFAULT_READER_MAX_RECORD_PENDING_COUNT);
    }

    @Override
    public void start() {
        LOGGER.info("Starting: {}", getClass().getSimpleName());
    }

    @Override
    public InputStatus pollNext(ReaderOutput<R> output) throws Exception {
        LOGGER.debug("Pooling next record");
        if (noMoreSplits) {
            LOGGER.info("There are no more splits");
            return InputStatus.END_OF_INPUT;
        }
        if (!splitFetched) {
            LOGGER.debug("Fetching splits");
            context.sendSplitRequest();
            splitFetched = true;
        }

        if (!currentSplits.isEmpty()) {
            fetchRecordsIfNeeded();

            if (!polledRecords.isEmpty()) {
                R executionRecord = polledRecords.removeFirst();
                emitRecord(output, executionRecord);
                if (isPendingLimitReached()) {
                    LOGGER.debug("Blocking reader due to hitting pending records limit");
                    blockReader();
                    return InputStatus.NOTHING_AVAILABLE;
                }
            }else {
                LOGGER.debug("Removing split: {} due to exhaustion of polled record set"
                        + ", after commit: {} records of: {} all commited, ",
                   currentSplit , currentSplitCommittedRecordCount, allCommittedRecordCount);
                currentSplits.removeFirst();
                splitFetched = false;
                polledRecords = null;
                //This is somehow reduntat to progress event, but shoudl correct situtation when
                //there were fetched less records than planned
                context.sendSourceEventToCoordinator(
                    new SplitCompletedEvent(currentSplit.splitId(), currentSplit.getLimit(), currentSplit.getEnumeratorId())
                );
                currentSplit = null;
                return InputStatus.MORE_AVAILABLE;
            }
        }
        return InputStatus.NOTHING_AVAILABLE;
    }

    private void emitRecord(ReaderOutput<R> output, R executionRecord) {
        currentRecordPendingCount++;
        int currentlyPendingForThisCheckpoint = 1;
        if (recordPendingCountPerCheckpoint.containsKey(currentCheckpointId)) {
            currentlyPendingForThisCheckpoint = recordPendingCountPerCheckpoint.get(currentCheckpointId);
            recordPendingCountPerCheckpoint.put(currentCheckpointId, ++currentlyPendingForThisCheckpoint);
        } else {
            recordPendingCountPerCheckpoint.put(currentCheckpointId, currentlyPendingForThisCheckpoint);
        }
        LOGGER.trace("Emitting record: {}", executionRecord);
        LOGGER.debug("There are {} records pending and {} pending for current checkpoint with id: {}",
            currentRecordPendingCount ,currentlyPendingForThisCheckpoint, currentCheckpointId);
        output.collect(executionRecord);
        currentSplit = currentSplit.withProgress(currentSplit.getProgress() + 1);
        currentSplits.set(0, currentSplit);
        emitProgressEvent();
    }

    private void emitProgressEvent() {
        if (currentSplit != null) {
            //TODO we could consider if we need to sent the event every time although it does not look as a big overhead.
            //Cause it is not every record but only every snapshot.
            ProgressSnapshotEvent progressEvent = new ProgressSnapshotEvent(currentCheckpointId,
                currentSplit.splitId(), currentSplit.getProgress(), currentSplit.getEnumeratorId());
            LOGGER.debug("Emitting progress event: {}", progressEvent);
            context.sendSourceEventToCoordinator(progressEvent);
        }

    }

    private void fetchRecordsIfNeeded() throws FlinkWorkflowException {
        currentSplit = currentSplits.getFirst();
        if (polledRecords == null) {
            LOGGER.debug("Fetching records from database");
            polledRecords = new LinkedList<>(fetchRecords());

            currentSplitCommittedRecordCount = 0;
        } else {
            LOGGER.debug("Already fetched records exist");
        }
    }

    protected abstract List<R> fetchRecords() throws FlinkWorkflowException;

    private boolean isPendingLimitReached() {
        if(currentRecordPendingCount >= maxRecordPending){
            LOGGER.debug("Pending limit: {} reached: {}", maxRecordPending, currentRecordPendingCount);
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) {
        LOGGER.debug("Checkpoint successfully finished on flink with id: {}", checkpointId);
        updatePendingRecordsState(checkpointId);
        if (currentRecordPendingCount < maxRecordPending) {
            unblockReader();
        }
    }

    @Override
    public List<DataPartition> snapshotState(long checkpointId) {
        LOGGER.info("Storing snapshot for checkpoint with id: {}, snapshot: {}", checkpointId, currentSplit);
        this.currentCheckpointId = checkpointId;
        return Optional.ofNullable(currentSplit).stream().toList();
    }

    @Override
    public CompletableFuture<Void> isAvailable() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Reader availability state: {} ", readerAvailable.state() == Future.State.SUCCESS ? "Not Blocked" : "Blocked");
        }
        return readerAvailable;
    }

    @Override
    public void addSplits(List<DataPartition> splits) {
        LOGGER.info("Adding splits: {}", splits);
        currentSplits.addAll(splits);
        readerAvailable.complete(null);
    }

    @Override
    public void notifyNoMoreSplits() {
        LOGGER.debug("Notified that there are no more splits");
        noMoreSplits = true;
        unblockReader();
    }

    @Override
    public void close() {
    }

    private void updatePendingRecordsState(long completedCheckpointId) {
        Set<Map.Entry<Long, Integer>> alreadyCommittedCheckpointSet = recordPendingCountPerCheckpoint.headMap(completedCheckpointId, false).entrySet();

        int committedRecordPendingCount = alreadyCommittedCheckpointSet
                .stream().map(Map.Entry::getValue)
                .reduce(0, Integer::sum);
        Set<Long> committedCheckpoints = alreadyCommittedCheckpointSet
                .stream().map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (committedRecordPendingCount > 0) {
            allCommittedRecordCount += committedRecordPendingCount;
            currentSplitCommittedRecordCount += committedRecordPendingCount;
            currentRecordPendingCount -= committedRecordPendingCount;
            LOGGER.debug("Pending records state updated successfully! Increased commited records by: {}"
                    + ", commited in current split: {}, all commited: {}"
                , committedRecordPendingCount, currentSplitCommittedRecordCount, allCommittedRecordCount);
        } else {
            LOGGER.debug("Pending records state did not changed for checkpoint with id: {} or less", completedCheckpointId);
        }
        recordPendingCountPerCheckpoint.keySet().removeAll(committedCheckpoints);
    }

    private void blockReader() {
        LOGGER.debug("Blocking the reader");
        readerAvailable = new CompletableFuture<>();
    }

    private void unblockReader() {
        LOGGER.debug("Unblocking the reader - current pending: {}", currentRecordPendingCount);
        readerAvailable.complete(null);
    }

}
