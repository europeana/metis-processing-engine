package eu.europeana.processing;

import static java.util.Collections.singleton;

import eu.europeana.processing.config.DbCleaningMode;
import eu.europeana.processing.config.TestConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DbCleaner {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String TEST_DATASET_QUERY_PARAMETER = "testDataset";

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TestConfigurationProperties config;

  @Autowired
  private TransactionTemplate transactionTemplate;

  private static final String[] TABLES = {
      "Execution_Record",
      "Execution_Record_Exception_Log",
      "Execution_Record_External_Identifier",
      "Task_Info",
  };

  private static final String[] REQUESTS_CLEANING_ALL_DB_BUT_PREVIOUS_STEPS_OF_TEST_DATASET = {
      "DELETE from \"batch-framework\".Execution_Record WHERE dataset_Id <> :testDataset OR execution_Id NOT IN :previousSteps",
      "DELETE from \"batch-framework\".Execution_Record_Exception_Log WHERE dataset_Id <> :testDataset OR execution_Id NOT IN :previousSteps",
      "DELETE from \"batch-framework\".Execution_Record_External_Identifier WHERE dataset_Id <> :testDataset OR execution_Id NOT IN :previousSteps",
      "DELETE from \"batch-framework\".Task_Info WHERE CAST(task_id as VARCHAR) NOT IN :previousSteps",
  };

  private static final String[] REQUESTS_CLEANING_CURRENT_AND_FOLLOWING_STEPS_OF_TEST_DATASET = {
      "DELETE from \"batch-framework\".Execution_Record WHERE dataset_Id = :testDataset AND execution_Id NOT IN :previousSteps",
      "DELETE from \"batch-framework\".Execution_Record_Exception_Log WHERE dataset_Id = :testDataset AND execution_Id NOT IN :previousSteps",
      "DELETE from \"batch-framework\".Execution_Record_External_Identifier WHERE dataset_Id = :testDataset AND execution_Id NOT IN :previousSteps",
      "DELETE from \"batch-framework\".Task_Info WHERE CAST(task_id as VARCHAR) NOT IN :previousSteps",
  };

  public void clearDbFor(int stepNumber) {
    if (config.getDbCleaning() == DbCleaningMode.NO_CLEANING) {
      LOGGER.warn("Cleaning data from previous tests is turned off: {}", stepNumber);
      return;
    }

    LOGGER.info("Clearing data for test for the workflow step number: {}", stepNumber);
    for (int tableNumber = 0; tableNumber < TABLES.length; tableNumber++) {
      clearTable(stepNumber, tableNumber);
    }
  }

  private void clearTable(int stepNumber, int tableNumber) {
    Integer result = transactionTemplate.execute(status -> {
      Query query;
      DbCleaningMode cleaningMode = config.getDbCleaning();
      if (cleaningMode == DbCleaningMode.CLEAN_ALL_DB_BUT_PREVIOUS_STEPS_OF_TEST_DATASET) {
        if (stepNumber == 1) {
          query = createTruncateWholeTableQuery(tableNumber);
        } else {
          query = createDeleteSelectedDataRequest(stepNumber, tableNumber,
              REQUESTS_CLEANING_ALL_DB_BUT_PREVIOUS_STEPS_OF_TEST_DATASET);
        }
      } else if (cleaningMode == DbCleaningMode.CLEAN_CURRENT_AND_FOLLOWING_STEPS_OF_TEST_DATASET) {
        query = createDeleteSelectedDataRequest(stepNumber, tableNumber,
            REQUESTS_CLEANING_CURRENT_AND_FOLLOWING_STEPS_OF_TEST_DATASET);
      } else {
        throw new UnsupportedOperationException("Method clearTable does not support cleaning mode: " + cleaningMode);
      }
      return query.executeUpdate();
    });
    LOGGER.info("Cleared data in the table: {}, removed {} rows.", TABLES[tableNumber], result);
  }

  private Query createDeleteSelectedDataRequest(int stepNumber, int tableNumber, String[] queriesSet) {

    Set<String> previousStepsSet = IntStream.range(1, stepNumber).mapToObj(String::valueOf)
                                            .collect(Collectors.toUnmodifiableSet());
    //It is needed cause the db and driver could not work with NOT IN with empty set. So we put an artificial value that not exists in DB.
    if (previousStepsSet.size() == 0) {
      previousStepsSet = singleton(UUID.randomUUID().toString());
    }
    String sqlString = queriesSet[tableNumber];
    Query query = entityManager
        .createNativeQuery(sqlString)
        .setParameter("previousSteps", previousStepsSet);
    if (sqlString.contains(":" + TEST_DATASET_QUERY_PARAMETER)) {
      query.setParameter(TEST_DATASET_QUERY_PARAMETER, config.getDatasetId());
    }

    return query;
  }

  private Query createTruncateWholeTableQuery(int tableNumber) {
    Query query;
    query = entityManager.createNativeQuery("TRUNCATE TABLE \"batch-framework\"." + TABLES[tableNumber]);
    return query;
  }
}
