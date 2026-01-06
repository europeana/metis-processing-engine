package eu.europeana.processing.repository;

import static org.assertj.db.api.Assertions.assertThat;

import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.exception.FlinkWorkflowException;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.TaskInfo;
import org.apache.flink.util.ParameterTool;
import org.assertj.core.api.Assertions;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Request.Builder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TaskInfoRepositoryTest extends RepositoryTest {


  private static Builder request;

  @BeforeAll
  static void before() {
    startPostgresDbServer();
    prepareRequests();
  }

  @Test
  void shouldSaveCorrectTaskInfo() throws FlinkWorkflowException {
    TaskInfoRepository taskInfoRepository = prepareRepository();

    taskInfoRepository.save(new TaskInfo(1, "name", null, null, null, 12, 14));
    taskInfoRepository.save(new TaskInfo(2, "name", null, null, null, 0, 10));
    taskInfoRepository.save(new TaskInfo(3, "name", null, null, null, 20, 30));

    assertThat(
        request.parameters(1).build()
    ).hasNumberOfRows(1)
     .row(0).value(TaskInfoRepository.TASK_ID_COL_NAME).isEqualTo(1)
     .row(0).value(TaskInfoRepository.COMMIT_COUNT_COL_NAME).isEqualTo(12)
     .row(0).value(TaskInfoRepository.WRITE_COUNT_COL_NAME).isEqualTo(14);

    assertThat(
        request.parameters(2).build()
    ).hasNumberOfRows(1)
     .row(0).value(TaskInfoRepository.TASK_ID_COL_NAME).isEqualTo(2)
     .row(0).value(TaskInfoRepository.COMMIT_COUNT_COL_NAME).isEqualTo(0)
     .row(0).value(TaskInfoRepository.WRITE_COUNT_COL_NAME).isEqualTo(10);

    assertThat(
        request.parameters(3).build()
    ).hasNumberOfRows(1)
     .row(0).value(TaskInfoRepository.TASK_ID_COL_NAME).isEqualTo(3)
     .row(0).value(TaskInfoRepository.COMMIT_COUNT_COL_NAME).isEqualTo(20)
     .row(0).value(TaskInfoRepository.WRITE_COUNT_COL_NAME).isEqualTo(30);
  }


  @Test
  void shouldUpdateCorrectTaskInfo() throws FlinkWorkflowException {
    TaskInfoRepository taskInfoRepository = prepareRepository();

    taskInfoRepository.save(new TaskInfo(10, "name", null, null, null, 12, 12));
    taskInfoRepository.update(new TaskInfo(10, "name", null, null, null, 12, 14));

    assertThat(
        request.parameters(10).build()
    ).hasNumberOfRows(1)
     .row(0).value(TaskInfoRepository.COMMIT_COUNT_COL_NAME).isEqualTo(12)
     .row(0).value(TaskInfoRepository.WRITE_COUNT_COL_NAME).isEqualTo(14);
  }

  @Test
  void shouldFindTaskById() throws FlinkWorkflowException {
    TaskInfoRepository taskInfoRepository = prepareRepository();

    taskInfoRepository.save(new TaskInfo(20, "name", null, null, null, 12, 12));

    Assertions.assertThat(taskInfoRepository.findById(20)).isPresent();
    Assertions.assertThat(taskInfoRepository.findById(30)).isNotPresent();
  }

  @Override
  public TaskInfoRepository prepareRepository() {
    return new TaskInfoRepository(
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

    request = assertDbConnection.request(
        "select * from \"batch-framework\".task_info where task_id = ?;"
    );
  }
}