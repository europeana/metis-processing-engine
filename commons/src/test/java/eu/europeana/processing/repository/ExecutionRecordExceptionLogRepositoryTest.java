package eu.europeana.processing.repository;

import static org.assertj.db.api.Assertions.assertThat;

import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.exception.FlinkWorkflowException;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordKey;
import eu.europeana.processing.model.ExecutionRecordResult;
import org.apache.flink.util.ParameterTool;
import org.assertj.core.api.Assertions;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Request.Builder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExecutionRecordExceptionLogRepositoryTest extends RepositoryTest {

  private static Builder executionRecordExceptionLogTableRequest;

  @BeforeAll
  static void before() {
    startPostgresDbServer();
    prepareRequests();
  }

  @Test
  void shouldSaveCorrectExecutionRecordExceptionLog() throws FlinkWorkflowException {
    ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository = prepareRepository();

    executionRecordExceptionLogRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .executionId("executionId")
                    .recordId("recordId")
                    .datasetId("datasetId")
                    .build())
                .build())
            .exception("Error")
            .build());

    assertThat(
        executionRecordExceptionLogTableRequest.parameters("datasetId", "executionId", "recordId").build()
    ).hasNumberOfRows(1)
     .row(0).value("exception").isEqualTo("Error");
  }

  @Test
  void shouldCountByDatasetIdAndExecutionId_1() throws FlinkWorkflowException {
    ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository = prepareRepository();

    long exceptions = executionRecordExceptionLogRepository.countByDatasetIdAndExecutionId("datasetId", "executionId_1");

    Assertions.assertThat(exceptions).isZero();
  }

  @Test
  void shouldCountByDatasetIdAndExecutionId_2() throws FlinkWorkflowException {
    ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository = prepareRepository();

    executionRecordExceptionLogRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .executionId("executionId_2")
                    .recordId("recordId")
                    .datasetId("datasetId")
                    .build())
                .build())
            .exception("Error")
            .build());

    executionRecordExceptionLogRepository.save(
        ExecutionRecordResult
            .builder()
            .executionRecord(ExecutionRecord
                .builder()
                .executionRecordKey(ExecutionRecordKey
                    .builder()
                    .executionId("executionId_2")
                    .recordId("recordId_1")
                    .datasetId("datasetId")
                    .build())
                .build())
            .exception("Error")
            .build());

    long exceptions = executionRecordExceptionLogRepository.countByDatasetIdAndExecutionId("datasetId", "executionId_2");

    Assertions.assertThat(exceptions).isEqualTo(2);
  }


  @Override
  public ExecutionRecordExceptionLogRepository prepareRepository() {
    return new ExecutionRecordExceptionLogRepository(
        new DbConnectionProvider(ParameterTool.fromArgs(
            new String[]{
                "-" + JobParamName.DATASOURCE_URL, postgres.getJdbcUrl(),
                "-" + JobParamName.DATASOURCE_USERNAME, "test",
                "-" + JobParamName.DATASOURCE_PASSWORD, "test"
            }
        ))
    );
  }

  public static void prepareRequests() {
    AssertDbConnection assertDbConnection = AssertDbConnectionFactory.of(postgres.getJdbcUrl(), postgres.getUsername(),
        postgres.getPassword()).create();

    executionRecordExceptionLogTableRequest = assertDbConnection.request(
        "select * from \"batch-framework\".execution_record_exception_log where dataset_id = ? and execution_id = ? and record_id =?;"
    );
  }
}