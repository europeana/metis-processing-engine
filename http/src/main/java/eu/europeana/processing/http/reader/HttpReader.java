package eu.europeana.processing.http.reader;

import static eu.europeana.processing.job.JobName.HTTP_HARVEST;
import static eu.europeana.processing.job.JobParamName.DATASET_ID;

import eu.europeana.processing.http.reader.exception.HttpSourceException;
import eu.europeana.processing.http.reader.extractor.ArchiveContentExtractor;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecord.ExecutionRecordBuilder;
import eu.europeana.processing.model.ExecutionRecordKey;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.model.ExecutionRecordResult.ExecutionRecordResultBuilder;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.util.ParameterTool;
import org.apache.flink.core.io.InputStatus;
import org.apache.flink.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SourceReader implementation for HttpSource. It emits content of the files from the compressed archive.
 * Every file is emitted as separate record.
 */
public class HttpReader implements SourceReader<ExecutionRecordResult, HttpSourceSplit> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpReader.class);
  private final SourceReaderContext context;
  private final String datasetId;
  private final String taskId;
  private ArchiveContentExtractor extractor;
  private HttpSourceSplit assignedSplit;
  private Iterator<String> fileNameIterator = Collections.emptyIterator();
  private CompletableFuture<Void> readerAvailable = new CompletableFuture<>();
  private boolean noMoreSplits;

  /**
   * Creates HttpReader
   * @param context - Flink engine SourceReaderContext context
   * @param parameterTool - all the command line parameters of the job
   */
  public HttpReader(SourceReaderContext context, ParameterTool parameterTool) {
    this.context = context;
    datasetId = parameterTool.getRequired(DATASET_ID);
    taskId = parameterTool.getRequired(JobParamName.TASK_ID);
  }

  @Override
  public void start() {
    LOGGER.info("Started {}" , this.getClass().getSimpleName());
  }

  @Override
  public InputStatus pollNext(ReaderOutput<ExecutionRecordResult> output) {
    LOGGER.debug("Pooling next record");
    if (noMoreSplits) {
      LOGGER.info("There are no more splits");
      return InputStatus.END_OF_INPUT;
    } else if (!fileNameIterator.hasNext()) {
      return orderNewSplitAndWait();
    } else {
      emitRecord(output, fileNameIterator.next());
      return InputStatus.MORE_AVAILABLE;
    }
  }

  private InputStatus orderNewSplitAndWait() {
    context.sendSplitRequest();
    blockReader();
    LOGGER.debug("Ordered new split from the enumerator.");
    return InputStatus.NOTHING_AVAILABLE;
  }

  private void emitRecord(ReaderOutput<ExecutionRecordResult> output, String fileName) {
    ExecutionRecordResult theRecord = prepareRecord(fileName);
    output.collect(theRecord);
    if(!fileNameIterator.hasNext()){
      wholeSplitEmitted();
    }
  }

  private ExecutionRecordResult prepareRecord(String fileName) {
    ExecutionRecordKey key = ExecutionRecordKey.builder().datasetId(datasetId).executionId(taskId).recordId(fileName).build();
    ExecutionRecordBuilder executionRecordBuilder = ExecutionRecord.builder().executionRecordKey(key).executionName(HTTP_HARVEST);
    ExecutionRecordResultBuilder executionRecordResultBuilder = ExecutionRecordResult.builder();

    try {
      executionRecordBuilder.recordData(extractor.getExtractedFileContent(fileName));
    } catch (HttpSourceException e) {
      LOGGER.warn("Error extracting file: {}", fileName, e);
      executionRecordBuilder.recordData("");
      executionRecordResultBuilder.exception(ExceptionUtils.stringifyException(e));
    }

    executionRecordResultBuilder.executionRecord(executionRecordBuilder.build());
    return executionRecordResultBuilder.build();
  }

  private void wholeSplitEmitted() {
    context.sendSourceEventToCoordinator(new SplitEmittedEvent(assignedSplit.splitId(), assignedSplit.getFileNames().size()));
    assignedSplit = null;
  }

  @Override
  public void addSplits(List<HttpSourceSplit> splits) {
    assertAllowOnlySingleSplitAssignment(splits);
    assignedSplit = splits.getFirst();
    if (extractor == null) {
      extractor = new ArchiveContentExtractor(assignedSplit.getExtractionMode(), assignedSplit.getDownloadedArchiveFile());
    }
    fileNameIterator = assignedSplit.getFileNames().iterator();
    unblockReader();
  }

  @Override
  public void notifyNoMoreSplits() {
    LOGGER.debug("Notified reader of task: {} about no more splits.", taskId);
    noMoreSplits = true;
    unblockReader();
  }

  @Override
  public List<HttpSourceSplit> snapshotState(long checkpointId) {
    List<HttpSourceSplit> snapshot = Optional.ofNullable(assignedSplit).stream().toList();
    LOGGER.debug("Created reader snapshot for task: {} for the checkpoint: {}, snapshot: {}",
        taskId, checkpointId, snapshot);
    return snapshot;
  }

  @Override
  public void notifyCheckpointComplete(long checkpointId) {
    LOGGER.debug("Reader of task: {}, notified about checkpoint: {} completion.", taskId, checkpointId);
  }

  @Override
  public void close() throws Exception {
    if (extractor != null) {
      extractor.close();
    }
  }

  private void assertAllowOnlySingleSplitAssignment(List<HttpSourceSplit> splits) {
    if ((assignedSplit != null) || splits.size() > 1) {
      throw new HttpSourceException("Cannot assign more than one split at once! currently assigned split: "
          + assignedSplit + " new splits to assign, " + splits);
    }
  }

  @Override
  public CompletableFuture<Void> isAvailable() {
    return readerAvailable;
  }

  private void blockReader() {
    LOGGER.debug("Blocking the reader");
    readerAvailable = new CompletableFuture<>();
  }

  private void unblockReader() {
    LOGGER.debug("Unblocking the reader");
    readerAvailable.complete(null);
  }

}
