package eu.europeana.processing.http.source;

import static eu.europeana.processing.job.JobName.OAI_HARVEST;
import static eu.europeana.processing.job.JobParamName.DATASET_ID;

import eu.europeana.processing.http.source.extractor.ArchiveContentExtractor;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecord.ExecutionRecordBuilder;
import eu.europeana.processing.model.ExecutionRecordKey;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.model.ExecutionRecordResult.ExecutionRecordResultBuilder;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.io.InputStatus;
import org.apache.flink.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpReader implements SourceReader<ExecutionRecordResult, HttpSourceSplit> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEnumerator.class);
  private final SourceReaderContext context;
  private final String datasetId;
  private final String taskId;
  private ArchiveContentExtractor extractor;
  private HttpSourceSplit assignedSplit;
  private Iterator<String> fileNameIterator = Collections.emptyIterator();
  private CompletableFuture<Void> readerAvailable = new CompletableFuture<>();
  private boolean noMoreSplits;

  public HttpReader(SourceReaderContext context, ParameterTool parameterTool) {
    this.context = context;
    datasetId = parameterTool.get(DATASET_ID);
    taskId = parameterTool.get(JobParamName.TASK_ID);
  }

  @Override
  public void start() {
    LOGGER.info("Started " + this.getClass().getSimpleName());
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
    assignedSplit = null;
    context.sendSplitRequest();
    blockReader();
    LOGGER.info("Ordered new split from the enumerator.");
    return InputStatus.NOTHING_AVAILABLE;
  }

  private void emitRecord(ReaderOutput<ExecutionRecordResult> output, String fileName) {
    ExecutionRecordKey key = ExecutionRecordKey.builder().datasetId(datasetId).executionId(taskId).recordId(fileName).build();
    ExecutionRecordBuilder executionRecordBuilder = ExecutionRecord.builder().executionRecordKey(key).executionName(OAI_HARVEST);
    ExecutionRecordResultBuilder executionRecordResultBuilder = ExecutionRecordResult.builder();

    try {
      executionRecordBuilder.recordData(extractor.getExtractedFileContent(fileName));
    } catch (IOException e) {
      LOGGER.warn("Error extracting file: {}", fileName, e);
      executionRecordBuilder.recordData("");
      executionRecordResultBuilder.exception(ExceptionUtils.stringifyException(e));
    }

    executionRecordResultBuilder.executionRecord(executionRecordBuilder.build());
    output.collect(executionRecordResultBuilder.build());
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
    LOGGER.info("Notified reader of task: {} about no more splits.", taskId);
    noMoreSplits = true;
    unblockReader();
  }

  @Override
  public List<HttpSourceSplit> snapshotState(long checkpointId) {
    List<HttpSourceSplit> snapshot = Optional.ofNullable(assignedSplit).stream().toList();
    LOGGER.info("Created reader snapshot for task: {} for the checkpoint: {}, snapshot: {}",
        taskId, checkpointId, snapshot);
    return snapshot;
  }

  @Override
  public void notifyCheckpointComplete(long checkpointId) {
    LOGGER.info("Reader of task: {}, notified about checkpoint: {} completion.", taskId, checkpointId);
  }

  @Override
  public void close() throws Exception {
    if (extractor != null) {
      extractor.close();
    }
  }

  private void assertAllowOnlySingleSplitAssignment(List<HttpSourceSplit> splits) {
    if ((assignedSplit != null) || splits.size() > 1) {
      throw new RuntimeException("Cannot assign more than one split at once! currently assigned split: "
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
