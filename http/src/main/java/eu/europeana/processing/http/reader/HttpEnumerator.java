package eu.europeana.processing.http.reader;

import eu.europeana.processing.http.reader.dowloader.HttpSourceFileDownloader;
import eu.europeana.processing.http.reader.extractor.ArchiveFileNamesExtractor;
import eu.europeana.processing.http.reader.extractor.ExtractionMode;
import eu.europeana.processing.job.JobParamName;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.flink.api.connector.source.SourceEvent;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.java.utils.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SplitEnumerator implementation for HttpSource. It downloads an archive file and provide splits containing lists of the
 * files from the archive and chosen extraction mode - depending on archive type. Depending on the archive type, the archive also is
 * or is not extracted to the shared temporary directory - more described in: {@link ArchiveFileNamesExtractor}
 */
public class HttpEnumerator implements SplitEnumerator<HttpSourceSplit, HttpEnumeratorState> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEnumerator.class);
  private static final int DEFAULT_CHUNK_SIZE = 1000;

  private final SplitEnumeratorContext<HttpSourceSplit> context;
  private final ParameterTool parameterTool;
  private final int chunkSize;
  private final long taskId;
  private final String jobDirectoryPath;
  private final String archiveUrl;
  private ProgressUpdater progressUpdater;
  private int startedFilesCount;
  private int emittedFilesCount;
  private Path downloadedFile;
  private ExtractionMode extractionMode;
  private Iterator<String> notStartedFilesIterator;
  private final List<HttpSourceSplit> returnedPartitions;
  private int allFileCount = -1;

  /**
   * Creates HttpEnumerator
   *
   * @param context - Flink engine SplitEnumeratorContext context
   * @param state - state of the enumerator from the checkpoint
   * @param parameterTool - all the command line parameters of the job
   * @param jobDirectoryPath - path of shared temporary folder for downloading and extracting archives for this job.
   */
  public HttpEnumerator(SplitEnumeratorContext<HttpSourceSplit> context, HttpEnumeratorState state,
      ParameterTool parameterTool, String jobDirectoryPath) {
    this.context = context;
    this.parameterTool = parameterTool;
    this.taskId = parameterTool.getLong(JobParamName.TASK_ID);
    this.jobDirectoryPath = jobDirectoryPath;
    this.chunkSize = parameterTool.getInt(JobParamName.CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
    this.archiveUrl = parameterTool.getRequired(JobParamName.HTTP_ARCHIVE_URL);

    if (state != null) {
      downloadedFile = Optional.ofNullable(state.getDownloadedFile()).map(Path::of).orElse(null);
      extractionMode = state.getExtractionMode();
      startedFilesCount = state.getStartedFilesCount();
      emittedFilesCount = state.getCompletedFilesCount();
      returnedPartitions = state.getReturnedPartitions();
    } else {
      downloadedFile = null;
      extractionMode = null;
      startedFilesCount = 0;
      emittedFilesCount = 0;
      returnedPartitions = new LinkedList<>();
    }
    LOGGER.info("Created enumerator for the http task id: {}. Previous state: {}", taskId, state);
  }


  @Override
  public void start() {
    LOGGER.debug("Starting HttpEnumerator for task id: {}, downloaded file: {}, extractionMode: {},"
            + " already started files count: {}, completed count files: {},  returned partitions: {}",
        taskId, downloadedFile, extractionMode, startedFilesCount, emittedFilesCount, returnedPartitions);

    progressUpdater = new ProgressUpdater(parameterTool, emittedFilesCount);

    downloadArchive();

    ArchiveFileNamesExtractor archiveFileNamesExtractor = new ArchiveFileNamesExtractor(downloadedFile, extractionMode);
    extractionMode = archiveFileNamesExtractor.extract();

    List<String> fileList = archiveFileNamesExtractor.getFileNames();
    allFileCount = fileList.size();
    notStartedFilesIterator = skipAlreadyStarted(fileList).iterator();

    LOGGER.info("Started HttpEnumerator for task id: {}. File count: {}, already started: {}",
        taskId, allFileCount, startedFilesCount);
  }

  @Override
  public void handleSplitRequest(int subtaskId, String requesterHostname) {
    LOGGER.debug("Split request from subtask: {} on host: {}", taskId, requesterHostname);

    HttpSourceSplit splitToBeServed;
    if (!returnedPartitions.isEmpty()) {
      splitToBeServed = returnedPartitions.removeFirst();
    } else if (notStartedFilesIterator.hasNext()) {
      splitToBeServed = createNextPartition();
    } else {

      LOGGER.info("No more remaining files to start extraction for task id: {}. Already started: {} files.",
          taskId, startedFilesCount);
      context.signalNoMoreSplits(subtaskId);
      return;
    }

    context.assignSplit(splitToBeServed, subtaskId);
    LOGGER.debug("Assigned split: {} for subtaskId: {}, host: {}. Already started {} of: {} files",
        splitToBeServed, subtaskId, requesterHostname, startedFilesCount, allFileCount);
  }

  @Override
  public void addSplitsBack(List<HttpSourceSplit> splits, int subtaskId) {
    LOGGER.info("Adding splits back from the subtask: {}, splits: {}", subtaskId, splits);
    returnedPartitions.addAll(splits);
  }

  @Override
  public HttpEnumeratorState snapshotState(long checkpointId) {
    HttpEnumeratorState state = HttpEnumeratorState.builder()
                                                   .downloadedFile(downloadedFile.toString())
                                                   .extractionMode(extractionMode)
                                                   .startedFilesCount(startedFilesCount)
                                                   //We set completedFilesCount in the snapshot using current value
                                                   //of emittedFilesCount. It is valid cause checkpoint that contains this
                                                   //snapshot is saved at the moment when all these records are completed.
                                                   //It is assured by Flink engine.
                                                   .completedFilesCount(emittedFilesCount)
                                                   .returnedPartitions(returnedPartitions)
                                                   .build();
    progressUpdater.snapshotEmittedFilesCount(emittedFilesCount);
    LOGGER.debug("Created snapshot of task: {} state for the checkpoint: {}. State: {}",
        taskId, checkpointId, state);
    return state;
  }

  private void downloadArchive() {
    if (downloadedFile == null) {
      downloadedFile = new HttpSourceFileDownloader(jobDirectoryPath, archiveUrl).download();
    }
  }

  private List<String> skipAlreadyStarted(List<String> fileList) {
    return fileList.subList(startedFilesCount, fileList.size());
  }

  private HttpSourceSplit createNextPartition() {
    ArrayList<String> fileNames = new ArrayList<>();
    for (int i = 0; i < chunkSize && notStartedFilesIterator.hasNext(); i++) {
      fileNames.add(notStartedFilesIterator.next());
    }

    HttpSourceSplit split = HttpSourceSplit.builder()
                                           .extractionMode(extractionMode)
                                           .downloadedArchiveFile(downloadedFile.toString())
                                           .fileNames(fileNames)
                                           .firstFileIndex(startedFilesCount)
                                           .build();

    startedFilesCount += fileNames.size();
    return split;
  }

  @Override
  public void handleSourceEvent(int subtaskId, SourceEvent sourceEvent) {
    LOGGER.debug("Received event: {} from subtask: {}", sourceEvent, subtaskId);
    if (sourceEvent instanceof SplitEmittedEvent splitEmittedEvent) {
      handleSplitEmittedEvent(splitEmittedEvent);
    }
  }

  private void handleSplitEmittedEvent(SplitEmittedEvent splitEmittedEvent) {
    emittedFilesCount += splitEmittedEvent.getSplitSize();
  }

  @Override
  public void notifyCheckpointComplete(long checkpointId) {
    LOGGER.debug("Task: {}, checkpoint: {} completed. Updating progress...", taskId, checkpointId);
    progressUpdater.saveProgressInDB();
  }

  @Override
  public void close() {
    LOGGER.info("Closing HttpEnumerator");
    progressUpdater.close();
  }

  //////////////////////////////////////////////////////////////////////////////////
  //    Not currently used methods from the interface - only logging events      ///

  /// ///////////////////////////////////////////////////////////////////////////////

  @Override
  public void addReader(int subtaskId) {
    LOGGER.debug("New reader added for, the subtaskId: {}", subtaskId);
  }

  @Override
  public void notifyCheckpointAborted(long checkpointId) {
    LOGGER.info("Checkpoint aborted: {}!", checkpointId);
  }

}
