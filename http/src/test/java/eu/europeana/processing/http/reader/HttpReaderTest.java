package eu.europeana.processing.http.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import eu.europeana.processing.http.reader.extractor.ExtractionMode;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordKey;
import eu.europeana.processing.model.ExecutionRecordResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future.State;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.shaded.guava33.com.google.common.collect.Lists;
import org.apache.flink.util.ParameterTool;
import org.apache.flink.core.io.InputStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpReaderTest extends AbstractUnpackingTest {

  public static final String TASK_ID = "1";
  public static final String DATASET_ID = "dataset-id";
  private static final String FILE1 = "ecloud_e2e_tests_without_4_records_ecloud_e2e_tests_NLS____NLS2__RS_664______2WGTWS8_sr.xml";
  private static final String FILE2 = "ecloud_e2e_tests_without_4_records_ecloud_e2e_tests_NLS____NLS2__RS_643______06VMZI9_sr.xml";

  @Mock
  private SourceReaderContext context;
  @Mock
  private ReaderOutput<ExecutionRecordResult> output;
  @Captor
  private ArgumentCaptor<ExecutionRecordResult> emitCaptor;

  private HttpReader reader;
  private Path zipFile;
  private Path extractedFile1;
  private Path extractedFile2;
  private Path badFilePath;
  private ExecutionRecordResult expectedEmittedRecordFromZip1;
  private ExecutionRecordResult expectedEmittedRecordFromZip2;
  private ExecutionRecordResult expectedEmittedExtractedRecord1;
  private ExecutionRecordResult expectedEmittedExtractedRecord2;


  @BeforeEach
  void setup() throws IOException {
    zipFile = copyFileToTempFolder("records.zip");
    extractedFile1 = copyFileToTempFolder(FILE1);
    extractedFile2 = copyFileToTempFolder(FILE2);
    badFilePath = tempDirectory.resolve("BadFile.xml");

    ParameterTool parameterTool = ParameterTool.fromMap(Map.of(
        JobParamName.TASK_ID, TASK_ID,
        JobParamName.DATASET_ID, DATASET_ID
    ));
    reader = new HttpReader(context, parameterTool);

    expectedEmittedRecordFromZip1 = createExecutionRecord(FILE1, extractedFile1);
    expectedEmittedRecordFromZip2 = createExecutionRecord(FILE2, extractedFile2);
    expectedEmittedExtractedRecord1 = createExecutionRecord(extractedFile1.toString(), extractedFile1);
    expectedEmittedExtractedRecord2 = createExecutionRecord(extractedFile2.toString(), extractedFile2);
  }


  @Test
  void shouldSendSplitRequestAndWaitForResponseOnFirstPoll() {
    InputStatus result = reader.pollNext(output);

    verify(context).sendSplitRequest();
    assertEquals(InputStatus.NOTHING_AVAILABLE, result);
    assertEquals(State.RUNNING, reader.isAvailable().state());
  }

  @Test
  void shouldEmitRecordsDirectlyFromZip() {
    reader.pollNext(output);
    reader.addSplits(
        List.of(
            HttpSourceSplit
                .builder()
                .downloadedArchiveFile(zipFile.toString())
                .extractionMode(ExtractionMode.ON_FLY_IN_MEMORY).fileNames(Lists.newArrayList(FILE1, FILE2))
                .build())
    );

    //RECORD1
    assertEquals(InputStatus.MORE_AVAILABLE, reader.pollNext(output));
    verify(output).collect(expectedEmittedRecordFromZip1);
    assertEquals(State.SUCCESS, reader.isAvailable().state());
    //RECORD2
    assertEquals(InputStatus.MORE_AVAILABLE, reader.pollNext(output));
    verify(output).collect(expectedEmittedRecordFromZip2);
    assertEquals(State.SUCCESS, reader.isAvailable().state());
    verify(context).sendSourceEventToCoordinator(new SplitEmittedEvent("0", 2));
    //NO MORE RECORDS:
    assertEquals(InputStatus.NOTHING_AVAILABLE, reader.pollNext(output));
    assertEquals(State.RUNNING, reader.isAvailable().state());
  }

  @Test
  void shouldEmitExtractedRecordsFromDirectory() {
    reader.pollNext(output);
    reader.addSplits(
        List.of(
            HttpSourceSplit
                .builder()
                .downloadedArchiveFile(zipFile.toString())
                .extractionMode(ExtractionMode.INITIAL_TO_DIRECTORY)
                .fileNames(Lists.newArrayList(extractedFile1.toString(), extractedFile2.toString()))
                .build())
    );

    //RECORD1
    assertEquals(InputStatus.MORE_AVAILABLE, reader.pollNext(output));
    verify(output).collect(expectedEmittedExtractedRecord1);
    assertEquals(State.SUCCESS, reader.isAvailable().state());
    //RECORD2
    assertEquals(InputStatus.MORE_AVAILABLE, reader.pollNext(output));
    verify(output).collect(expectedEmittedExtractedRecord2);
    assertEquals(State.SUCCESS, reader.isAvailable().state());
    verify(context).sendSourceEventToCoordinator(new SplitEmittedEvent("0", 2));
    //NO MORE RECORDS:
    assertEquals(InputStatus.NOTHING_AVAILABLE, reader.pollNext(output));
    assertEquals(State.RUNNING, reader.isAvailable().state());
  }

  @Test
  void shouldEmitFailedRecordIfCouldNotGetContentOfTheFile() {
    reader.pollNext(output);
    reader.addSplits(
        List.of(
            HttpSourceSplit
                .builder()
                .downloadedArchiveFile(zipFile.toString())
                .extractionMode(ExtractionMode.INITIAL_TO_DIRECTORY).fileNames(Lists.newArrayList(badFilePath.toString()))
                .build())
    );

    assertEquals(InputStatus.MORE_AVAILABLE, reader.pollNext(output));
    verify(output).collect(emitCaptor.capture());
    assertEquals(State.SUCCESS, reader.isAvailable().state());
    assertEquals(badFilePath.toString(), emitCaptor.getValue().getRecordId());
    assertNotNull(emitCaptor.getValue().getException());
    verify(context).sendSourceEventToCoordinator(new SplitEmittedEvent("0", 1));
  }

  @Test
  void shouldReactOnNoMoreSplitSignal() {
    reader.pollNext(output);
    reader.notifyNoMoreSplits();
    assertEquals(InputStatus.END_OF_INPUT, reader.pollNext(output));
  }

  @Test
  void shouldProperlySnapshotStateWhenSplitAssigned() {
    HttpSourceSplit split = HttpSourceSplit
        .builder()
        .downloadedArchiveFile(zipFile.toString())
        .extractionMode(ExtractionMode.ON_FLY_IN_MEMORY).fileNames(Lists.newArrayList(FILE1, FILE2))
        .build();

    reader.pollNext(output);
    reader.addSplits(List.of(split));

    List<HttpSourceSplit> snapshot = reader.snapshotState(0);
    assertEquals(List.of(split), snapshot);
  }

  @Test
  void shouldProperlySnapshotStateWhenSplitNotAssigned() {
    List<HttpSourceSplit> snapshot = reader.snapshotState(0);

    assertTrue(snapshot.isEmpty());
  }

  @Test
  void shouldNotFailOnNotUsedNotifications() {
    reader.start();

    reader.notifyCheckpointComplete(0);

    verifyNoInteractions(context, output);
  }

  @AfterEach
  void cleanup() throws Exception {
    reader.close();
  }

  private ExecutionRecordResult createExecutionRecord(String recordId, Path extractedFilePath) throws IOException {
    ExecutionRecordKey key = ExecutionRecordKey.builder().datasetId(DATASET_ID).executionId(TASK_ID).recordId(recordId).build();
    String fileContent = Files.readString(extractedFilePath);
    ExecutionRecord theRecord = ExecutionRecord.builder()
                                               .executionRecordKey(key).executionName(JobName.HTTP_HARVEST)
                                               .recordData(fileContent).build();
    return ExecutionRecordResult.builder().executionRecord(theRecord).build();
  }

}