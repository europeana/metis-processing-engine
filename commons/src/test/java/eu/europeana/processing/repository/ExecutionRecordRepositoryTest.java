package eu.europeana.processing.repository;

import static org.assertj.db.api.Assertions.assertThat;

import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordKey;
import eu.europeana.processing.model.ExecutionRecordResult;
import java.io.IOException;
import java.util.List;
import org.apache.flink.util.ParameterTool;
import org.assertj.core.api.Assertions;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Request.Builder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExecutionRecordRepositoryTest extends RepositoryTest {

  private static Builder executionRecordTableRequest;

  @BeforeAll
  static void before() {
    startPostgresDbServer();
    prepareRequests();
  }

  @Test
  void shouldCorrectlySaveOneExecutionRecordToDB() throws IOException {
    ExecutionRecordRepository executionRecordRepository = prepareRepository();

    executionRecordRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .datasetId("example-dataset-ID")
                    .executionId("example-execution-ID")
                    .recordId("example-record-ID")
                    .build())
                .executionName("Example-execution")
                .recordData("Example-record-data")
                .build())
            .build());

    assertThat(
        executionRecordTableRequest.parameters("example-dataset-ID", "example-execution-ID").build()
    ).hasNumberOfRows(1)
     .row(0).value(ExecutionRecordRepository.DATASET_ID_COL_NAME).isEqualTo("example-dataset-ID")
     .row(0).value(ExecutionRecordRepository.EXECUTION_ID_COL_NAME).isEqualTo("example-execution-ID");
  }

  public ExecutionRecordRepository prepareRepository() {
    return new ExecutionRecordRepository(
        new DbConnectionProvider(ParameterTool.fromArgs(
            new String[]{
                "-" + JobParamName.DATASOURCE_URL, postgres.getJdbcUrl(),
                "-" + JobParamName.DATASOURCE_USERNAME, "test",
                "-" + JobParamName.DATASOURCE_PASSWORD, "test"
            }
        ))
    );
  }

  @Test
  void shouldCorrectlySaveTwoExecutionRecordsToDB() throws IOException {
    ExecutionRecordRepository executionRecordRepository = prepareRepository();

    executionRecordRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .datasetId("example-dataset-ID-1")
                    .executionId("example-execution-ID-1")
                    .recordId("example-record-ID-1")
                    .build())
                .executionName("Example-execution-1")
                .recordData("Example-record-data-1")
                .build())
            .build());

    executionRecordRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .datasetId("example-dataset-ID-2")
                    .executionId("example-execution-ID-2")
                    .recordId("example-record-ID-2")
                    .build())
                .executionName("Example-execution-2")
                .recordData("Example-record-data-2")
                .build())
            .build());

    assertThat(
        executionRecordTableRequest.parameters("example-dataset-ID-1", "example-execution-ID-1").build()
    ).hasNumberOfRows(1)
     .row(0).value(ExecutionRecordRepository.DATASET_ID_COL_NAME).isEqualTo("example-dataset-ID-1")
     .row(0).value(ExecutionRecordRepository.EXECUTION_ID_COL_NAME).isEqualTo("example-execution-ID-1");

    assertThat(
        executionRecordTableRequest.parameters("example-dataset-ID-2", "example-execution-ID-2").build()
    ).hasNumberOfRows(1)
     .row(0).value(ExecutionRecordRepository.DATASET_ID_COL_NAME).isEqualTo("example-dataset-ID-2")
     .row(0).value(ExecutionRecordRepository.EXECUTION_ID_COL_NAME).isEqualTo("example-execution-ID-2");
  }

  @Test
  void shouldCorrectlySaveTwoExecutionRecordsFromOneExecutionToDB() throws IOException {
    ExecutionRecordRepository executionRecordRepository = prepareRepository();

    executionRecordRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .datasetId("example-dataset-ID-1")
                    .executionId("example-execution-ID-1")
                    .recordId("example-record-ID-1")
                    .build())
                .executionName("Example-execution-1")
                .recordData("Example-record-data-1")
                .build())
            .build());

    executionRecordRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .datasetId("example-dataset-ID-1")
                    .executionId("example-execution-ID-1")
                    .recordId("example-record-ID-2")
                    .build())
                .executionName("Example-execution-1")
                .recordData("Example-record-data-1")
                .build())
            .build());

    assertThat(
        executionRecordTableRequest.parameters("example-dataset-ID-1", "example-execution-ID-1").build()
    ).hasNumberOfRows(2)
     .row(0).value(ExecutionRecordRepository.DATASET_ID_COL_NAME).isEqualTo("example-dataset-ID-1")
     .row(0).value(ExecutionRecordRepository.EXECUTION_ID_COL_NAME).isEqualTo("example-execution-ID-1")
     .row(0).value(ExecutionRecordRepository.RECORD_ID_COL_NAME).isEqualTo("example-record-ID-1")

     .row(1).value(ExecutionRecordRepository.DATASET_ID_COL_NAME).isEqualTo("example-dataset-ID-1")
     .row(1).value(ExecutionRecordRepository.EXECUTION_ID_COL_NAME).isEqualTo("example-execution-ID-1")
     .row(1).value(ExecutionRecordRepository.RECORD_ID_COL_NAME).isEqualTo("example-record-ID-2");

  }

  @Test
  void shouldCountByDatasetIdAndExecutionId() throws IOException {
    ExecutionRecordRepository executionRecordRepository = prepareRepository();

    executionRecordRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .executionId("example-execution-ID-5")
                    .recordId("example-record-ID")
                    .datasetId("example-dataset-ID")
                    .build())
                .executionName("Example-execution")
                .recordData("Example-record-data")
                .build())
            .build());

    executionRecordRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .executionId("example-execution-ID-5")
                    .recordId("example-record-ID-1")
                    .datasetId("example-dataset-ID")
                    .build())
                .executionName("Example-execution")
                .recordData("Example-record-data-1")
                .build())
            .build());

    long records = executionRecordRepository.countByDatasetIdAndExecutionId("example-dataset-ID", "example-execution-ID-5");
    Assertions.assertThat(records).isEqualTo(2);
  }


  @Test
  void shouldRetrieveRecordsFromDB() throws IOException {
    ExecutionRecordRepository executionRecordRepository = prepareRepository();

    executionRecordRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .executionId("example-execution-ID-5")
                    .recordId("example-record-ID")
                    .datasetId("example-dataset-ID")
                    .build())
                .executionName("Example-execution")
                .recordData("Example-record-data")
                .build())
            .build());

    executionRecordRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .executionId("example-execution-ID-5")
                    .recordId("example-record-ID-1")
                    .datasetId("example-dataset-ID")
                    .build())
                .executionName("Example-execution")
                .recordData("Example-record-data-1")
                .build())
            .build());

    List<ExecutionRecord> byDatasetIdAndExecutionIdAndOffsetAndLimit = executionRecordRepository.getByDatasetIdAndExecutionIdAndOffsetAndLimit(
        "example-dataset-ID", "example-execution-ID-5", 0, 5);

    Assertions.assertThat(byDatasetIdAndExecutionIdAndOffsetAndLimit).hasSize(2);
  }

  public static void prepareRequests() {
    AssertDbConnection assertDbConnection = AssertDbConnectionFactory.of(postgres.getJdbcUrl(), postgres.getUsername(),
        postgres.getPassword()).create();

    executionRecordTableRequest = assertDbConnection.request(
        "select * from \"batch-framework\".execution_record where dataset_id = ? and execution_id = ?;"
    );
  }
}