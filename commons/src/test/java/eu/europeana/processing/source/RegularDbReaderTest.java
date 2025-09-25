package eu.europeana.processing.source;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.DataPartition;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.repository.DbRepository;
import eu.europeana.processing.repository.ExecutionRecordRepository;
import eu.europeana.processing.repository.RepositoryTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.core.io.InputStatus;
import org.apache.flink.util.ParameterTool;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegularDbReaderTest extends RepositoryTest {

  @Mock
  private SourceReaderContext mockContext;

  @Mock
  private ReaderOutput<ExecutionRecord> readerOutput;

  @BeforeAll
  static void before() {
    startPostgresDbServer();
  }

  @Override
  public DbRepository prepareRepository() {
    return null;
  }

  @Test
  void shoudlSignalEndOfInputWhenThereIsNoMoreSplits() throws Exception {
    //given
    ExecutionRecordRepository mock = Mockito.mock(ExecutionRecordRepository.class);

    try (RegularDbReader reader = new RegularDbReader(null, ParameterTool.fromArgs(
        new String[]{
            "-" + JobParamName.DATASOURCE_URL, postgres.getJdbcUrl(),
            "-" + JobParamName.DATASOURCE_USERNAME, "test",
            "-" + JobParamName.DATASOURCE_PASSWORD, "test"
        }
    ), mock)) {
      //when
      reader.notifyNoMoreSplits();
      InputStatus inputStatus = reader.pollNext(null);
      //then
      Assertions.assertThat(inputStatus).isEqualTo(InputStatus.END_OF_INPUT);
    }
  }

  @Test
  void shouldSendSplitRequestAndEmitZeroRecordsBecauseThereAreNoSplits() throws Exception {
    //given
    ExecutionRecordRepository mock = Mockito.mock(ExecutionRecordRepository.class);

    try (RegularDbReader reader = new RegularDbReader(mockContext, ParameterTool.fromArgs(
        new String[]{
            "-" + JobParamName.DATASOURCE_URL, postgres.getJdbcUrl(),
            "-" + JobParamName.DATASOURCE_USERNAME, "test",
            "-" + JobParamName.DATASOURCE_PASSWORD, "test"
        }
    ), mock)) {
      //when
      InputStatus inputStatus = reader.pollNext(readerOutput);

      //then
      Mockito.verify(readerOutput, Mockito.times(0)).collect(Mockito.any());
      Mockito.verify(mockContext, Mockito.times(1)).sendSplitRequest();
      Assertions.assertThat(inputStatus).isEqualTo(InputStatus.NOTHING_AVAILABLE);
    }
  }

  @Test
  void shouldSendSplitRequestAndEmitOneRecordFromSplitContainingOneRecord() throws Exception {

    //given
    ExecutionRecordRepository mock = Mockito.mock(ExecutionRecordRepository.class);

    Mockito.doReturn(Collections.singletonList(
               ExecutionRecord
                   .builder()
                   .executionName("sample execution name")
                   .build())).when(mock)
           .getByDatasetIdAndExecutionIdAndOffsetAndLimit(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
               Mockito.anyLong());

    try (RegularDbReader reader = new RegularDbReader(mockContext, ParameterTool.fromArgs(
        new String[]{
            "-" + JobParamName.DATASOURCE_URL, postgres.getJdbcUrl(),
            "-" + JobParamName.DATASOURCE_USERNAME, "test",
            "-" + JobParamName.DATASOURCE_PASSWORD, "test",
            "-" + JobParamName.DATASET_ID, "datasetId",
            "-" + JobParamName.EXECUTION_ID, "executionId",
        }
    ), mock)) {
      //when
      reader.addSplits(List.of(new DataPartition(0, 1, 0, null)));
      InputStatus inputStatus = reader.pollNext(readerOutput);

      //then
      Mockito.verify(readerOutput, Mockito.times(1)).collect(Mockito.any());
      Mockito.verify(mockContext, Mockito.times(1)).sendSplitRequest();
      Assertions.assertThat(inputStatus).isEqualTo(InputStatus.NOTHING_AVAILABLE);
    }
  }

  @Test
  void shouldSendOnlyOneSplitRequestAndEmitTwoRecordFromSplitContainingTwoRecords() throws Exception {
    //given
    ExecutionRecordRepository mock = Mockito.mock(ExecutionRecordRepository.class);

    Mockito.doReturn(Arrays.asList(
        ExecutionRecord
            .builder()
            .executionName("sample execution name")
            .build(),
        ExecutionRecord
            .builder()
            .executionName("sample execution name")
            .build())
    ).when(mock).getByDatasetIdAndExecutionIdAndOffsetAndLimit(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
        Mockito.anyLong());

    try (RegularDbReader reader = new RegularDbReader(mockContext, ParameterTool.fromArgs(
        new String[]{
            "-" + JobParamName.DATASOURCE_URL, postgres.getJdbcUrl(),
            "-" + JobParamName.DATASOURCE_USERNAME, "test",
            "-" + JobParamName.DATASOURCE_PASSWORD, "test",
            "-" + JobParamName.DATASET_ID, "datasetId",
            "-" + JobParamName.EXECUTION_ID, "executionId",
        }
    ), mock)) {
      //when
      reader.addSplits(List.of(
          new DataPartition(0, 5, 0, null)
      ));
      reader.pollNext(readerOutput);
      InputStatus inputStatus = reader.pollNext(readerOutput);

      //then
      Mockito.verify(mockContext, Mockito.times(1)).sendSplitRequest();
      Mockito.verify(readerOutput, Mockito.times(2)).collect(Mockito.any());
      Assertions.assertThat(inputStatus).isEqualTo(InputStatus.NOTHING_AVAILABLE);
    }
  }

  @Test
  void shouldSendOnlyOneSplitRequestAndEmitTwoRecordFromSplitContainingTwoRecords_1() throws Exception {
    //given
    ExecutionRecordRepository mock = Mockito.mock(ExecutionRecordRepository.class);

    Mockito.doReturn(Arrays.asList(
        ExecutionRecord
            .builder()
            .executionName("sample execution name")
            .build(),
        ExecutionRecord
            .builder()
            .executionName("sample execution name")
            .build())
    ).when(mock).getByDatasetIdAndExecutionIdAndOffsetAndLimit(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
        Mockito.anyLong());

    try (RegularDbReader reader = new RegularDbReader(mockContext, ParameterTool.fromArgs(
        new String[]{
            "-" + JobParamName.DATASOURCE_URL, postgres.getJdbcUrl(),
            "-" + JobParamName.DATASOURCE_USERNAME, "test",
            "-" + JobParamName.DATASOURCE_PASSWORD, "test",
            "-" + JobParamName.DATASET_ID, "datasetId",
            "-" + JobParamName.EXECUTION_ID, "executionId"
        }
    ), mock)) {
      //when
      reader.addSplits(List.of(
          new DataPartition(0, 5, 0, null)
      ));
      reader.pollNext(readerOutput);
      reader.pollNext(readerOutput);
      InputStatus inputStatus = reader.pollNext(readerOutput);

      //then
      Mockito.verify(mockContext, Mockito.times(1)).sendSplitRequest();
      Mockito.verify(readerOutput, Mockito.times(2)).collect(Mockito.any());
      Assertions.assertThat(inputStatus).isEqualTo(InputStatus.MORE_AVAILABLE);
    }
  }

  @Test
  void shouldBlockTheReaderAfterReachingPendingLimit() throws Exception {
    //given
    ExecutionRecordRepository mock = Mockito.mock(ExecutionRecordRepository.class);

    Mockito.doReturn(Arrays.asList(
        ExecutionRecord
            .builder()
            .executionName("sample execution name")
            .build(),
        ExecutionRecord
            .builder()
            .executionName("sample execution name")
            .build())
    ).when(mock).getByDatasetIdAndExecutionIdAndOffsetAndLimit(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
        Mockito.anyLong());

    try (RegularDbReader reader = new RegularDbReader(mockContext, ParameterTool.fromArgs(
        new String[]{
            "-" + JobParamName.DATASOURCE_URL, postgres.getJdbcUrl(),
            "-" + JobParamName.DATASOURCE_USERNAME, "test",
            "-" + JobParamName.DATASOURCE_PASSWORD, "test",
            "-" + JobParamName.DATASET_ID, "datasetId",
            "-" + JobParamName.EXECUTION_ID, "executionId",
            "-" + JobParamName.MAX_RECORD_PENDING, "1"
        }
    ), mock)) {
      //when
      reader.addSplits(List.of(
          new DataPartition(0, 5, 0, null)
      ));
      InputStatus inputStatus = reader.pollNext(readerOutput);
      //then
      Assertions.assertThat(inputStatus).isEqualTo(InputStatus.NOTHING_AVAILABLE);

      CompletableFuture<Void> available = reader.isAvailable();
      Assertions.assertThat(available.isDone()).isFalse();

      Mockito.verify(mockContext, Mockito.times(1)).sendSplitRequest();
      Mockito.verify(readerOutput, Mockito.times(1)).collect(Mockito.any());
    }
  }
}