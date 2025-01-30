package eu.europeana.processing.http.source;

import static eu.europeana.processing.http.source.extractor.ArchiveHeaderExtractor.EXTRACTED_SUB_DIR_NAME;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import eu.europeana.processing.http.source.extractor.ExtractionMode;
import eu.europeana.processing.job.JobParamName;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class HttpEnumeratorTest {

  private static final int SUBTASK0_ID = 0;
  private static final int SUBTASK1_ID = 1;
  private static final String WORKER_HOST = "task-manager-1";

  private static final String ZIP_FILE_URL = "https://metis-repository-rest.test.eanadev.org/repository/zip/ecloud_e2e_tests_without_4_records.zip";
  private static final String ZIP_FILE_NAME = "ecloud_e2e_tests_without_4_records.zip";
  private static final String FILE1 = "ecloud_e2e_tests_without_4_records_ecloud_e2e_tests_NLS____NLS2__RS_664______2WGTWS8_sr.xml";
  private static final String FILE2 = "ecloud_e2e_tests_without_4_records_ecloud_e2e_tests_NLS____NLS2__RS_643______06VMZI9_sr.xml";
  private static final String FILE3 = "ecloud_e2e_tests_without_4_records_ecloud_e2e_tests_NLS____NLS2__RS_486______3OL0PS4_sr.xml";
  private static final String FILE4 = "ecloud_e2e_tests_without_4_records_ecloud_e2e_tests_NLS____NLS2__RS_388______0Y4FH46_sr.xml";

  private static final String TGZ_URL = "http://ftp.eanadev.org/uploads/ESounds_Odessa.tgz";
  private static final String TGZ_FILE_NAME = "ESounds_Odessa.tgz";
  private static final String FILE1_INSIDE_TGZ = "24-04-02_14_54_53/Item_19541240.xml";


  @Mock
  private SplitEnumeratorContext<HttpSourceSplit> context;

  private String jobDirectory;
  private ParameterTool parameterTool;
  private String downLoadedFile;
  private HttpSourceSplit expectedSplit1;
  private HttpSourceSplit expectedSplit2;
  private Path tempDirectory;

  @BeforeEach
  public void setup() throws Exception {
    tempDirectory = Files.createTempDirectory(HttpEnumeratorTest.class.getSimpleName());
    jobDirectory = tempDirectory.resolve("task-dir").toString();
    downLoadedFile = Path.of(jobDirectory).resolve(ZIP_FILE_NAME).toString();
    parameterTool = ParameterTool.fromMap(Map.of(
        JobParamName.HTTP_ARCHIVE_URL,
        ZIP_FILE_URL,
        JobParamName.TASK_ID, "1",
        JobParamName.CHUNK_SIZE, "3"
    ));
    expectedSplit1 = HttpSourceSplit.builder()
                                    .downloadedArchiveFile(downLoadedFile)
                                    .extractionMode(ExtractionMode.ON_FLY_IN_MEMORY)
                                    .fileNames(List.of(FILE1, FILE2, FILE3))
                                    .firstFileIndex(0)
                                    .build();
    expectedSplit2 = HttpSourceSplit.builder()
                                    .downloadedArchiveFile(downLoadedFile)
                                    .extractionMode(ExtractionMode.ON_FLY_IN_MEMORY)
                                    .fileNames(List.of(FILE4))
                                    .firstFileIndex(3)
                                    .build();
    expectedSplit2 = HttpSourceSplit.builder()
                                    .downloadedArchiveFile(downLoadedFile)
                                    .extractionMode(ExtractionMode.ON_FLY_IN_MEMORY)
                                    .fileNames(List.of(FILE4))
                                    .firstFileIndex(3)
                                    .build();
  }

  @Test
  public void shouldAssignSplitsForRegularZip() {
    try (HttpEnumerator enumerator = new HttpEnumerator(context, null, parameterTool, jobDirectory)) {
      enumerator.start();
      enumerator.handleSplitRequest(SUBTASK0_ID, WORKER_HOST);
      enumerator.handleSplitRequest(SUBTASK1_ID, WORKER_HOST);
    }

    verify(context).assignSplit(expectedSplit1, SUBTASK0_ID);
    verify(context).assignSplit(expectedSplit2, SUBTASK1_ID);
  }

  @Test
  public void shouldAssignSplitsForTarFile() {
    downLoadedFile = Path.of(jobDirectory).resolve(TGZ_FILE_NAME).toString();
    parameterTool = ParameterTool.fromMap(Map.of(
        JobParamName.HTTP_ARCHIVE_URL,
        TGZ_URL,
        JobParamName.TASK_ID, "1"
    ));

    try (HttpEnumerator enumerator = new HttpEnumerator(context, null, parameterTool, jobDirectory)) {
      enumerator.start();
      enumerator.handleSplitRequest(SUBTASK0_ID, WORKER_HOST);
      enumerator.handleSplitRequest(SUBTASK1_ID, WORKER_HOST);
    }

    HttpSourceSplit expectedTarSplit =
        HttpSourceSplit.builder()
                       .downloadedArchiveFile(downLoadedFile)
                       .extractionMode(ExtractionMode.INITIAL_TO_DIRECTORY)
                       .fileNames(List.of(Path.of(jobDirectory).resolve(EXTRACTED_SUB_DIR_NAME).resolve(FILE1_INSIDE_TGZ).toString()))
                       .firstFileIndex(0)
                       .build();
    verify(context).assignSplit(expectedTarSplit, SUBTASK0_ID);
    verify(context, never()).assignSplit(any(), eq(SUBTASK1_ID));
  }

  @Test
  public void shouldProperlyDetectWhenAllSplitStarted() {
    try (HttpEnumerator enumerator = new HttpEnumerator(context, null, parameterTool, jobDirectory)) {
      enumerator.start();
      enumerator.handleSplitRequest(SUBTASK0_ID, WORKER_HOST);
      enumerator.handleSplitRequest(SUBTASK1_ID, WORKER_HOST);
      enumerator.handleSplitRequest(SUBTASK0_ID, WORKER_HOST);
      enumerator.handleSplitRequest(SUBTASK1_ID, WORKER_HOST);
    }

    InOrder inOrder = inOrder(context);
    inOrder.verify(context).assignSplit(expectedSplit1, SUBTASK0_ID);
    inOrder.verify(context).assignSplit(expectedSplit2, SUBTASK1_ID);
    inOrder.verify(context).signalNoMoreSplits(SUBTASK0_ID);
    inOrder.verify(context).signalNoMoreSplits(SUBTASK1_ID);
  }

  @Test
  public void shouldAssignSplitsWhenFileIsDownloadedIncompletely() throws IOException {
    createIncompleteDownloadedFile();
    HttpEnumeratorState state = HttpEnumeratorState.builder().returnedPartitions(new LinkedList<>()).build();

    try (HttpEnumerator enumerator = new HttpEnumerator(context, state, parameterTool, jobDirectory)) {
      enumerator.start();
      enumerator.handleSplitRequest(SUBTASK0_ID, WORKER_HOST);
      enumerator.handleSplitRequest(SUBTASK1_ID, WORKER_HOST);
    }

    verify(context).assignSplit(expectedSplit1, SUBTASK0_ID);
    verify(context).assignSplit(expectedSplit2, SUBTASK1_ID);
  }


  @Test
  public void shouldAssignSplitsWhenFileIsAlreadyDownloadedCompletely() throws IOException {
    createCompleteDownloadedFile();
    HttpEnumeratorState state = HttpEnumeratorState.builder().downloadedFile(downLoadedFile)
                                                   .returnedPartitions(new LinkedList<>()).build();

    try (HttpEnumerator enumerator = new HttpEnumerator(context, state, parameterTool, jobDirectory)) {
      enumerator.start();
      enumerator.handleSplitRequest(SUBTASK0_ID, WORKER_HOST);
      enumerator.handleSplitRequest(SUBTASK1_ID, WORKER_HOST);
    }

    verify(context).assignSplit(expectedSplit1, SUBTASK0_ID);
    verify(context).assignSplit(expectedSplit2, SUBTASK1_ID);
  }

  @Test
  public void shouldAssignOnlyNotStartedSplits() throws IOException {
    createCompleteDownloadedFile();
    HttpEnumeratorState state = HttpEnumeratorState.builder().downloadedFile(downLoadedFile)
                                                   .extractionMode(ExtractionMode.ON_FLY_IN_MEMORY)
                                                   .startedFilesCount(3)
                                                   .returnedPartitions(new LinkedList<>()).build();

    try (HttpEnumerator enumerator = new HttpEnumerator(context, state, parameterTool, jobDirectory)) {
      enumerator.start();
      enumerator.handleSplitRequest(SUBTASK0_ID, WORKER_HOST);
      enumerator.handleSplitRequest(SUBTASK1_ID, WORKER_HOST);
    }

    verify(context).assignSplit(expectedSplit2, SUBTASK0_ID);
    verify(context, never()).assignSplit(any(), eq(SUBTASK1_ID));
  }

  @Test
  public void shouldAssignSplitReturnedBackOnTaskManagerFail() throws IOException {
    createCompleteDownloadedFile();
    HttpEnumeratorState state = HttpEnumeratorState.builder().downloadedFile(downLoadedFile)
                                                   .extractionMode(ExtractionMode.ON_FLY_IN_MEMORY)
                                                   .startedFilesCount(4)
                                                   .returnedPartitions(new LinkedList<>()).build();

    try (HttpEnumerator enumerator = new HttpEnumerator(context, state, parameterTool, jobDirectory)) {
      enumerator.start();
      enumerator.addSplitsBack(List.of(expectedSplit2), SUBTASK1_ID);
      enumerator.handleSplitRequest(SUBTASK0_ID, WORKER_HOST);
      enumerator.handleSplitRequest(SUBTASK1_ID, WORKER_HOST);
    }

    verify(context).assignSplit(expectedSplit2, SUBTASK0_ID);
    verify(context, never()).assignSplit(any(), eq(SUBTASK1_ID));
  }

  @Test
  public void shouldProperlySnapshotState() throws IOException {
    createCompleteDownloadedFile();
    HttpEnumeratorState initialState = HttpEnumeratorState.builder().downloadedFile(downLoadedFile)
                                                          .extractionMode(ExtractionMode.ON_FLY_IN_MEMORY)
                                                          .startedFilesCount(2)
                                                          .returnedPartitions(List.of(expectedSplit1)).build();

    HttpEnumeratorState snapshot;
    try (HttpEnumerator enumerator = new HttpEnumerator(context, initialState, parameterTool, jobDirectory)) {
      enumerator.start();
      snapshot = enumerator.snapshotState(1);
    }

    assertEquals(initialState, snapshot);
  }

  @Test
  public void shouldNotFailOnNotUsedNotifications() throws IOException {
    createCompleteDownloadedFile();
    HttpEnumeratorState state = HttpEnumeratorState.builder().downloadedFile(downLoadedFile)
                                                   .extractionMode(ExtractionMode.ON_FLY_IN_MEMORY)
                                                   .build();

    try (HttpEnumerator enumerator = new HttpEnumerator(context, state, parameterTool, jobDirectory)) {
      enumerator.start();
      enumerator.addReader(SUBTASK0_ID);
      enumerator.notifyCheckpointComplete(0);
      enumerator.notifyCheckpointAborted(0);
    }

  }

  private void createCompleteDownloadedFile() throws IOException {
    createDownloadedFile("/records.zip");
  }

  private void createIncompleteDownloadedFile() throws IOException {
    createDownloadedFile("/empty.zip");
  }

  private void createDownloadedFile(String name) throws IOException {
    Files.createDirectory(Path.of(jobDirectory));
    IOUtils.copy(requireNonNull(HttpEnumeratorTest.class.getResourceAsStream(name))
        , new FileOutputStream(downLoadedFile));
  }

  @AfterEach
  public void cleanup() throws IOException {
    FileUtils.deleteDirectory(tempDirectory.toFile());
  }

}