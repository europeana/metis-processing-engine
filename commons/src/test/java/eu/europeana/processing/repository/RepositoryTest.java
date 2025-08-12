package eu.europeana.processing.repository;

import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class RepositoryTest {

  protected static PostgreSQLContainer<?> postgres;

  protected static void startPostgresDbServer() {

    postgres = new PostgreSQLContainer<>("postgres:15").withDatabaseName("metis-test")
                                                       .withUsername("test").withPassword("test")
                                                       .withInitScript("metis-processing-schema.sql");
    postgres.start();
  }

  @AfterAll
  static void after() {
    postgres.stop();
  }

  public abstract DbRepository prepareRepository();

}
