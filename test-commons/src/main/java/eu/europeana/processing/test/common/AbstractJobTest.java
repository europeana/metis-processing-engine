package eu.europeana.processing.test.common;

import static org.assertj.db.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Request;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractJobTest {

  public static final int MIN_PRACTICAL_XML_SIZE = 3000;
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJobTest.class);
  private PostgreSQLContainer<?> postgres;
  private Request result;
  private Request error;


  protected void startPostgresDbServer(String... dataInitiationScripts) {

    postgres = new PostgreSQLContainer<>("postgres:15").withDatabaseName("metis-test")
                                                       .withUsername("test").withPassword("test")
                                                       .withInitScripts(createInitiationScriptList(dataInitiationScripts));
    postgres.start();
    AssertDbConnection assertDbConnection = AssertDbConnectionFactory.of(postgres.getJdbcUrl(), postgres.getUsername(),
        postgres.getPassword()).create();
    result = assertDbConnection.request(
        "select * from \"batch-framework\".execution_record where execution_id='" + stepNumber() + "'").build();
    error = assertDbConnection.request(
        "select * from \"batch-framework\".execution_record_exception_log where execution_id='" + stepNumber() + "'").build();

  }

  protected abstract int stepNumber();

  protected String[] prepareArgs(String... specificParameters) {
    List<String> paremeters = new ArrayList<>();

    paremeters.add("--datasetId");
    paremeters.add("one-record");

    if (stepNumber() > 0) {
      paremeters.add("--executionId");
      paremeters.add(String.valueOf(stepNumber() - 1));
    }

    paremeters.add("--taskId");
    paremeters.add(String.valueOf(stepNumber()));

    paremeters.add("--datasource.url");
    paremeters.add(postgres.getJdbcUrl());
    paremeters.add("--datasource.username");
    paremeters.add(postgres.getUsername());
    paremeters.add("--datasource.password");
    paremeters.add(postgres.getPassword());
    paremeters.add("--chunkSize");
    paremeters.add("100");
    paremeters.add("--READER_PARALLELISM");
    paremeters.add("1");
    paremeters.add("--OPERATOR_PARALLELISM");
    paremeters.add("1");
    paremeters.add("--SINK_PARALLELISM");
    paremeters.add("1");

    paremeters.addAll(Arrays.asList(specificParameters));
    return paremeters.toArray(new String[0]);
  }


  @AfterEach
  void stopPostgres() {
    postgres.stop();
  }

  private static @NotNull List<String> createInitiationScriptList(String[] dataInitiationScripts) {
    List<String> initiationScripts = new ArrayList<>();
    initiationScripts.add("metis-processing-schema.sql");
    initiationScripts.addAll(Arrays.asList(dataInitiationScripts));
    return initiationScripts;
  }

  protected void assertThatResultRowIsSavedInDb() {
    assertThat(result).hasNumberOfRows(1);
    Assertions.assertThat(getResultXml()).hasSizeGreaterThan(MIN_PRACTICAL_XML_SIZE);
  }


  protected void assertThatErrorIsSavedInDb() {
    assertThat(error).hasNumberOfRows(1);
    LOGGER.info("Error saved in db: {}", getErrorMessage());
  }

  protected void assertNoErrorsSavedInDb() {
    assertThat(error).hasNumberOfRows(0);
  }

  private String getResultXml() {
    return result.getRow(0).getColumnValue("record_data").getValue().toString();
  }

  private String getErrorMessage() {
    return error.getRow(0).getColumnValue("exception").getValue().toString();
  }
}
