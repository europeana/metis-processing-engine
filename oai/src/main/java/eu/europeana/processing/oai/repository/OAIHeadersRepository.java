package eu.europeana.processing.oai.repository;

import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.repository.DbRepository;
import eu.europeana.processing.retryable.Retryable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database repository responsible for <b>execution_record_external_identifier</b> table
 */
@Retryable(delay = 5000, maxAttempts = 5)
public class OAIHeadersRepository implements DbRepository {

    private static final String NO_OF_ELEMENTS =
        """
            select count(*) as elements
                from "batch-framework".execution_record_external_identifier
                where dataset_id = ? and execution_id = ?
        """;

    private static final String LIMIT =
        """
            select * 
                from "batch-framework".execution_record_external_identifier 
                where dataset_id = ? and execution_id = ? and record_index >= ? and record_index < ?;
        """;

    private static final Logger LOGGER = LoggerFactory.getLogger(OAIHeadersRepository.class);

    private final DbConnectionProvider dbConnectionProvider;

    /**
     * Default constructor needed for byte-buddy proxy
     */
    public OAIHeadersRepository() {
        dbConnectionProvider = null;
    }

    /**
     * Constructor used by repositories
     *
     * @param dbConnectionProvider database connection details
     */
    public OAIHeadersRepository(DbConnectionProvider dbConnectionProvider) {
        this.dbConnectionProvider = dbConnectionProvider;
    }

    /**
     * Saves the {@link OaiRecordHeader} in <b>execution_record_external_identifier</b> table
     * <br/>
     * In case of conflict (try to insert the same header twice) the method does nothing.
     *
     * @param datasetId dataset identifier
     * @param executionId execution identifier
     * @param header instance to be saved in the database
     * @param index index of saved header record
     * @return true - if record was saved, false in case of constraint violation
     * @throws IOException in case of any DB exception
     */
    public boolean save(String datasetId, String executionId, OaiRecordHeader header, int index) throws IOException {

        try (Connection con = dbConnectionProvider.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(
                 "INSERT INTO \"batch-framework\".execution_record_external_identifier (DATASET_ID,EXECUTION_ID,RECORD_ID,is_deleted,datestamp,record_index)"
                 + " VALUES (?,?,?,?,?,?) ON CONFLICT (DATASET_ID,EXECUTION_ID, RECORD_ID) DO NOTHING")) {

            preparedStatement.setString(1, datasetId);
            preparedStatement.setString(2, executionId);
            preparedStatement.setString(3, header.getOaiIdentifier());
            preparedStatement.setBoolean(4, header.isDeleted());
            java.sql.Timestamp timestamp= Optional.ofNullable(header.getDatestamp()).map(java.sql.Timestamp::from).orElse(null);
            preparedStatement.setTimestamp(5, timestamp);
            preparedStatement.setInt(6, index);
            int modifiedRowCount = preparedStatement.executeUpdate();

            if (modifiedRowCount == 0) {
                LOGGER.info("Header record already existed in the DB: {}", header);
            }
            return modifiedRowCount > 0;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    /**
     * Counts all the records in the <b>execution_record_external_identifier</b> table based on the provided parameters
     *
     * @param datasetId dataset identifier
     * @param executionId execution identifier
     * @return number of elements in <b>execution_record_external_identifier</b> table for specified dataset and execution
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
     * Provides sub-list of oll {@link OaiRecordHeader} instances for given execution and dataset
     * @param datasetId dataset identifier
     * @param executionId execution identifier
     * @param offset dataset offset
     * @param limit dataset limit
     * @return list of all {@link OaiRecordHeader} fulfilling provided criteria
     * @throws IOException in case of any DB exception
     */
    public List<OaiRecordHeader> getByDatasetIdAndExecutionIdAndOffsetAndLimit(
        String datasetId,
        String executionId,
        long offset,
        long limit) throws IOException {
        try (Connection con = dbConnectionProvider.getConnection();
             PreparedStatement preparedStatement = con.prepareStatement(LIMIT)) {

            preparedStatement.setString(1, datasetId);
            preparedStatement.setString(2, executionId);
            preparedStatement.setLong(3, offset);
            preparedStatement.setLong(4, offset+limit);

            List<OaiRecordHeader> result = new ArrayList<>();
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Instant datestamp=Optional.ofNullable(resultSet.getTimestamp("datestamp"))
                                          .map(Timestamp::toInstant).orElse(null);
                result.add(new OaiRecordHeader(
                    resultSet.getString("record_id"),
                    resultSet.getBoolean("is_deleted"),
                    datestamp));
            }
            return result;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
