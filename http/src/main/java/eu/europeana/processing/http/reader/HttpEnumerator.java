package eu.europeana.processing.http.reader;

import eu.europeana.processing.http.reader.dowloader.HttpSourceFileDownloader;
import eu.europeana.processing.http.reader.extractor.ArchiveFileNamesExtractor;
import eu.europeana.processing.http.reader.extractor.ExtractionMode;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.source.AbstractEnumerator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.util.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SplitEnumerator implementation for HttpSource. It downloads an archive file and provide splits containing lists of the
 * files from the archive and chosen extraction mode - depending on archive type. Depending on the archive type, the archive also
 * is or is not extracted to the shared temporary directory - more described in: {@link ArchiveFileNamesExtractor}
 */
public class HttpEnumerator extends AbstractEnumerator<HttpSourceSplit, HttpEnumeratorState> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEnumerator.class);

  private final String jobDirectoryPath;
  private final String archiveUrl;
  private Path downloadedFile;
  private ExtractionMode extractionMode;
  private Iterator<String> notStartedFilesIterator;

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
    super(context, parameterTool, state);
    this.jobDirectoryPath = jobDirectoryPath;
    this.archiveUrl = parameterTool.getRequired(JobParamName.HTTP_ARCHIVE_URL);

    if (state != null) {
      downloadedFile = Optional.ofNullable(state.getDownloadedFile()).map(Path::of).orElse(null);
      extractionMode = state.getExtractionMode();
    } else {
      downloadedFile = null;
      extractionMode = null;
    }
    LOGGER.info("Created enumerator for the http task id: {}. Previous state: {}", taskId, state);
  }


  @Override
  public void start() {
    LOGGER.debug("Starting HttpEnumerator for task id: {}, downloaded file: {}, extractionMode: {},"
            + " already started files count: {}, completed count files: {},  returned partitions: {}",
        taskId, downloadedFile, extractionMode, startedRecordsCount, emittedRecordCount, returnedPartitions);
    super.start();

    downloadArchive();

    ArchiveFileNamesExtractor archiveFileNamesExtractor = new ArchiveFileNamesExtractor(downloadedFile, extractionMode);
    extractionMode = archiveFileNamesExtractor.extract();

    List<String> fileList = archiveFileNamesExtractor.getFileNames();
    recordsToBeProcessed = fileList.size();
    notStartedFilesIterator = skipAlreadyStarted(fileList).iterator();

    LOGGER.info("Started HttpEnumerator for task id: {}. File count: {}, already started: {}",
        taskId, recordsToBeProcessed, startedRecordsCount);
  }

  @Override
  protected void createDbRepositories() {
  }

  @Override
  protected HttpEnumeratorState createState() {
    HttpEnumeratorState state = new HttpEnumeratorState();
    state.setDownloadedFile(downloadedFile.toString());
    state.setExtractionMode(extractionMode);
    return state;
  }

  private void downloadArchive() {
    if (downloadedFile == null) {
      downloadedFile = new HttpSourceFileDownloader(jobDirectoryPath, archiveUrl).download();
    }
  }

  private List<String> skipAlreadyStarted(List<String> fileList) {
    return fileList.subList((int) startedRecordsCount, fileList.size());
  }

  protected HttpSourceSplit createNextPartition() {
    ArrayList<String> fileNames = new ArrayList<>();
    for (int i = 0; i < chunkSize && notStartedFilesIterator.hasNext(); i++) {
      fileNames.add(notStartedFilesIterator.next());
    }

    if (!fileNames.isEmpty()) {
      HttpSourceSplit split = HttpSourceSplit.builder()
                                             .extractionMode(extractionMode)
                                             .downloadedArchiveFile(downloadedFile.toString())
                                             .fileNames(fileNames)
                                             .firstFileIndex(startedRecordsCount)
                                             .build();

      startedRecordsCount += fileNames.size();
      return split;
    }else{
      return null;
    }
  }

}
