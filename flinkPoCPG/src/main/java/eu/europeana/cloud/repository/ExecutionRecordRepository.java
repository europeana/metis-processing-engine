package eu.europeana.cloud.repository;

import eu.europeana.cloud.model.ExecutionRecord;
import eu.europeana.cloud.model.ExecutionRecordKey;
import eu.europeana.cloud.model.ExecutionRecordResult;
import eu.europeana.cloud.tool.DbConnectionProvider;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExecutionRecordRepository implements DbRepository, Serializable {

    private final DbConnectionProvider dbConnectionProvider;

    private static final String NO_OF_ELEMENTS = "select count(*) as elements from \"batch-framework\".execution_record where dataset_id = ? and execution_id = ?";
    private static final String LIMIT = "select * from \"batch-framework\".execution_record where dataset_id = ? and execution_id = ? offset ? limit ?;";

    public ExecutionRecordRepository(DbConnectionProvider dbConnectionProvider) {
        this.dbConnectionProvider = dbConnectionProvider;
    }

    public void save(ExecutionRecordResult executionRecordResult) throws IOException {

        try (Connection con = dbConnectionProvider.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO \"batch-framework\".execution_record (DATASET_ID,EXECUTION_ID,EXECUTION_NAME, RECORD_ID, RECORD_DATA) VALUES (?,?,?,?,?)")) {

            preparedStatement.setString(1, executionRecordResult.getExecutionRecord().getExecutionRecordKey().getDatasetId());
            preparedStatement.setString(2, executionRecordResult.getExecutionRecord().getExecutionRecordKey().getExecutionId());
            preparedStatement.setString(3, executionRecordResult.getExecutionRecord().getExecutionName());
            preparedStatement.setString(4, executionRecordResult.getExecutionRecord().getExecutionRecordKey().getRecordId());
            preparedStatement.setString(5, executionRecordResult.getExecutionRecord().getRecordData());
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

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

    //TODO to be changed, returned list may be really big
    public List<ExecutionRecord> getByDatasetIdAndExecutionIdAndOffsetAndLimit(String datasetId, String executionId, long offset, long limit) throws IOException {
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