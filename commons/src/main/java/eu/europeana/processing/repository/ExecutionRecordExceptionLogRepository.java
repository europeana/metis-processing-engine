package eu.europeana.processing.repository;

import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.retryable.Retryable;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database repository responsible for <b>execution_record_exception_log</b> table
 */
@Retryable(delay = 5000, maxAttempts = 5)
public class ExecutionRecordExceptionLogRepository implements DbRepository, Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionRecordExceptionLogRepository.class);

    private static final String NO_OF_ELEMENTS = "select count(*) as elements from \"batch-framework\".execution_record_exception_log where dataset_id = ? and execution_id = ?";

    private final DbConnectionProvider dbConnectionProvider;

    /**
     * Default constructor needed for byte-buddy proxy
     */
    public ExecutionRecordExceptionLogRepository() {
        dbConnectionProvider = null;
    }

    /**
     * Constructor used by repositories
     * @param dbConnectionProvider database connection details
     */
    public ExecutionRecordExceptionLogRepository(DbConnectionProvider dbConnectionProvider) {
        this.dbConnectionProvider = dbConnectionProvider;
    }

    /**
     * Saves the {@link ExecutionRecordResult} in <b>execution_record_exception_log</b> table
     * @param executionRecordResult instance to be saved in the database
     */
    public void save(ExecutionRecordResult executionRecordResult) {
        try (Connection con = dbConnectionProvider.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(
                     "INSERT INTO \"batch-framework\".execution_record_exception_log (DATASET_ID,EXECUTION_ID,EXECUTION_NAME, RECORD_ID, exception)"
                         + " VALUES (?,?,?,?,?) ON CONFLICT (DATASET_ID, EXECUTION_ID, RECORD_ID) DO NOTHING")
        ) {

            ExecutionRecord executionRecord = executionRecordResult.getExecutionRecord();
            preparedStatement.setString(1, executionRecord.getExecutionRecordKey().getDatasetId());
            preparedStatement.setString(2, executionRecord.getExecutionRecordKey().getExecutionId());
            preparedStatement.setString(3, executionRecord.getExecutionName());
            preparedStatement.setString(4, executionRecord.getExecutionRecordKey().getRecordId());
            preparedStatement.setString(5, executionRecordResult.getException());
            int modifiedRowCount = preparedStatement.executeUpdate();

            if (modifiedRowCount == 0) {
                LOGGER.info("Record error log already existed in the DB: {}", executionRecord.getExecutionRecordKey());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Counts all the records in the <b>execution_record_exception_log</b> table based on the provided parameters
     * @param datasetId dataset identifier
     * @param executionId execution identifier
     * @return number of elements in <b>execution_record_exception_log</b> table for specified dataset and execution
     * @throws IOException
     */
    public long countByDatasetIdAndExecutionId(String datasetId, String executionId) throws IOException {

        ResultSet resultSet;
        try (PreparedStatement preparedStatement = dbConnectionProvider.getConnection().prepareStatement(NO_OF_ELEMENTS)) {
            preparedStatement.setString(1, datasetId);
            preparedStatement.setString(2, executionId);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getLong("elements");
            } else {
                return 0L;
            }
        } catch(SQLException e){
            throw new IOException(e);
        }
    }

}
