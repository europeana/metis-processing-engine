package eu.europeana.processing;

import com.zaxxer.hikari.HikariConfig;
import eu.europeana.processing.config.OaiSourceConfigurationProperties;
import eu.europeana.processing.config.TestConfigurationProperties;
import eu.europeana.processing.config.TestsConfig;
import eu.europeana.processing.config.db.entity.ExecutionRecord;
import eu.europeana.processing.config.db.entity.ExecutionRecordExceptionLog;
import eu.europeana.processing.config.db.repositories.ExecutionRecordExceptionLogRepository;
import eu.europeana.processing.config.db.repositories.ExecutionRecordRepository;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.apache.commons.lang3.time.StopWatch;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {TestsConfig.class})
@EnableAutoConfiguration
@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class AbstractPerformanceTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  protected HikariConfig dbConfig;


  @Resource
  protected ExecutionRecordRepository<ExecutionRecord> executionRecordRepository;

  @Resource
  protected ExecutionRecordExceptionLogRepository<ExecutionRecordExceptionLog> executionRecordExceptionLogRepository;

  private static boolean firstTest = true;

  private static boolean cleared;

  protected StopWatch startWatch;

  @Autowired
  private DbCleaner dbCleaner;

  @Autowired
  protected OaiSourceConfigurationProperties sourceProperties;

  @Autowired
  protected TestConfigurationProperties testProperties;

  //Could not use original jupiter's @BeforeEach because we need to pass test number parameter
  protected void beforeEach(int stepNumber) throws InterruptedException {
    if (firstTest) {
      firstTest = false;
      dbCleaner.clearDbFor(stepNumber);
      cleared = true;
    } else {
      if (cleared) {
        waitBetweenTests();
        LOGGER.info("There is no need to clear DB. It was cleared before first test.");
      } else {
        throw new RuntimeException("The DB could not be cleared before first test. Could not execute following tests.");
      }
      LOGGER.info("Executing test for workflow step no: {}", stepNumber);
    }
  }

  private void waitBetweenTests() throws InterruptedException {
    if (testProperties.getPauseBetweenTestsMs() > 0) {
      LOGGER.info("Waiting {} milliseconds between test executions...", testProperties.getPauseBetweenTestsMs());
      Thread.sleep(testProperties.getPauseBetweenTestsMs());
    }
  }

  protected void validateResult(int stepNumber) throws IOException {
    LOGGER.info("Step: {} - task execution time: {}", stepNumber, startWatch.formatTime());
    String datasetId = testProperties.getDatasetId();
    String taskId = String.valueOf(stepNumber);
    int expectedRecordCount = stepNumber != 1 ? sourceProperties.getValidRecordCount() : sourceProperties.getRecordCount() ;
    long expectedErrorCount = stepNumber != 2 ? 0: sourceProperties.getRecordCount() - sourceProperties.getValidRecordCount();
    Assertions.assertThat(executionRecordRepository.countByDatasetIdAndExecutionId(datasetId, taskId)).isEqualTo(expectedRecordCount);
    Assertions.assertThat(executionRecordExceptionLogRepository.countByDatasetIdAndExecutionId(datasetId, taskId)).isEqualTo(expectedErrorCount);
  }

}
