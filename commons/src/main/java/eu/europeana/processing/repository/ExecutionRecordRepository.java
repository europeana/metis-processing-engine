package eu.europeana.processing.repository;

import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordKey;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.retryable.Retryable;
import java.io.Serial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Database repository responsible for <b>execution_record</b> table
 */
@Retryable(delay = 5000, maxAttempts = 5)
public class ExecutionRecordRepository implements DbRepository, Serializable {

    @Serial
    private static final long serialVersionUID = 1;

    private static final String NO_OF_ELEMENTS =
        """
            select count(*) as elements
                from "batch-framework".execution_record
                where dataset_id = ? and execution_id = ?
        """;

    private static final String LIMIT =
        """
            select * 
                from "batch-framework".execution_record 
                where dataset_id = ? and execution_id = ? offset ? limit ?;
        """;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionRecordRepository.class);

    private final DbConnectionProvider dbConnectionProvider;

    /**
     * Default constructor needed for byte-buddy proxy
     */
    public ExecutionRecordRepository() {
        dbConnectionProvider = null;
    }

    /**
     * Constructor used by repositories
     *
     * @param dbConnectionProvider database connection details
     */
    public ExecutionRecordRepository(DbConnectionProvider dbConnectionProvider) {
        this.dbConnectionProvider = dbConnectionProvider;
    }

    /**
     * Saves the {@link ExecutionRecordResult} in <b>execution_record</b> table
     *
     * @param executionRecordResult instance to be saved in the database
     * @throws IOException in case of any DB exception
     */
    public void save(ExecutionRecordResult executionRecordResult) throws IOException {

        try (Connection con = dbConnectionProvider.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(
                 "INSERT INTO \"batch-framework\".execution_record (DATASET_ID,EXECUTION_ID,EXECUTION_NAME, RECORD_ID, RECORD_DATA)"
                 + " VALUES (?,?,?,?,?) ON CONFLICT (DATASET_ID,EXECUTION_ID, RECORD_ID) DO NOTHING")) {

            ExecutionRecord executionRecord = executionRecordResult.getExecutionRecord();
            preparedStatement.setString(1, executionRecord.getExecutionRecordKey().getDatasetId());
            preparedStatement.setString(2, executionRecord.getExecutionRecordKey().getExecutionId());
            preparedStatement.setString(3, executionRecord.getExecutionName());
            preparedStatement.setString(4, executionRecord.getExecutionRecordKey().getRecordId());
            preparedStatement.setString(5, executionRecord.getRecordData());
            int modifiedRowCount = preparedStatement.executeUpdate();

            if(modifiedRowCount==0){
                LOGGER.info("Execution record already existed in the DB: {}", executionRecord.getExecutionRecordKey());
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    /**
     * Counts all the records in the <b>execution_record</b> table based on the provided parameters
     *
     * @param datasetId dataset identifier
     * @param executionId execution identifier
     * @return number of elements in <b>execution_record</b> table for specified dataset and execution
     * @throws IOException in case of any DB exception
     */
    public long countByDatasetIdAndExecutionId(String datasetId, String executionId) throws IOException {

        ResultSet resultSet;
        try (Connection con = dbConnectionProvider.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(NO_OF_ELEMENTS)) {
            preparedStatement.setString(1, datasetId);
            preparedStatement.setString(2, executionId);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getLong("elements");
            } else {
                return 0L;
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    /**
     * Provides sub-list of oll {@link ExecutionRecord} instances for given execution and dataset
     * @param datasetId dataset identifier
     * @param executionId execution identifier
     * @param offset dataset offset
     * @param limit dataset limit
     * @return list of all {@link ExecutionRecord} fulfilling provided criteria
     * @throws IOException in case of any DB exception
     */
    //TODO to be changed, returned list may be really big
    public List<ExecutionRecord> getByDatasetIdAndExecutionIdAndOffsetAndLimit(
        String datasetId,
        String executionId,
        long offset,
        long limit) throws IOException {
        try (Connection con = dbConnectionProvider.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(LIMIT)) {

            preparedStatement.setString(1, datasetId);
            preparedStatement.setString(2, executionId);
            preparedStatement.setLong(3, offset);
            preparedStatement.setLong(4, limit);

            List<ExecutionRecord> result = new ArrayList<>();
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                result.add(
                        ExecutionRecord.builder()
                                .executionRecordKey(
                                        ExecutionRecordKey.builder()
                                                .datasetId(resultSet.getString("dataset_id"))
                                                .executionId(resultSet.getString("execution_id"))
                                                .recordId(resultSet.getString("record_id"))
                                                .build())
                                .executionName(resultSet.getString("execution_name"))
                                .recordData(new String(resultSet.getBytes("record_data")))
                                .build()
                );
            }
            return result;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
