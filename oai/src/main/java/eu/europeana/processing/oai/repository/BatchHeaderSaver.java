package eu.europeana.processing.oai.repository;

import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for batch saving record headers in PostgresDB.
 */
public class BatchHeaderSaver {

  private static final Logger LOGGER = LoggerFactory.getLogger(BatchHeaderSaver.class);

  public static final int MAX_BATCH_SIZE = 100;
  private static final long MAX_DURATION_BETWEEN_BATCH_SAVES = Duration.ofSeconds(2).toMillis();

  private final OAIHeadersRepository repository;
  private final String datasetId;
  private final String executionId;
  private final boolean savedHeadersInPreviousJobExecutions;

  @Getter
  private int allRecordsInDb;
  @Getter
  private int newHeaders;
  private List<OaiRecordHeader> headersToSave = new ArrayList<>();
  private StopWatch durationBetweenBatchSavesWatch = StopWatch.createStarted();

  /**
   * Creates batch saver
   *
   * @param repository - db repository
   * @param datasetId - dataset id
   * @param executionId - execution id
   * @param headersFromPreviousExecutionsCount - number of headers in db from previous execution
   */
  public BatchHeaderSaver(OAIHeadersRepository repository, String datasetId, String executionId,
      int headersFromPreviousExecutionsCount) {
    this.repository = repository;
    this.datasetId = datasetId;
    this.executionId = executionId;
    this.savedHeadersInPreviousJobExecutions = headersFromPreviousExecutionsCount > 0;
    this.allRecordsInDb = headersFromPreviousExecutionsCount;
  }

  /**
   * Saves header, it buffers it and then saves in batch if batch limit achieved or time limit elapse after last save.
   *
   * @param oaiHeader - header
   * @return true, if the header was really saved at this moment
   * @throws IOException - in case of problems with DB
   */
  public boolean save(OaiRecordHeader oaiHeader) throws IOException {
    headersToSave.add(oaiHeader);
    if (headersToSave.size() >= MAX_BATCH_SIZE || durationBetweenBatchSavesWatch.getTime() >= MAX_DURATION_BETWEEN_BATCH_SAVES) {
      return flush();
    } else {
      return false;
    }
  }

  /**
   * Saves headers that were buffered and not saved yet
   *
   * @return true - if the headers were really saved: they were in buffer and were not in the DB already.
   * @throws IOException - in case of problems with DB
   */
  public boolean flush() throws IOException {

    if (savedHeadersInPreviousJobExecutions) {
      //We need only check it if the job was restarted because of fail-over and there are already saved headers in DB
      List<String> identifiers = headersToSave.stream().map(OaiRecordHeader::getOaiIdentifier).toList();
      Set<String> existing = repository.getExistingIdentifiers(datasetId, executionId, identifiers);
      headersToSave = headersToSave.stream().filter(header -> !existing.contains(header.getOaiIdentifier()))
          .collect(Collectors.toCollection(ArrayList::new)); //ArrayList is needed here because it is later modified
    }

    if (headersToSave.isEmpty()) {
      durationBetweenBatchSavesWatch = StopWatch.createStarted();
      return false;
    }

    repository.save(datasetId, executionId, headersToSave, allRecordsInDb);
    LOGGER.debug("Saved {} headers id DB.", headersToSave.size());
    allRecordsInDb += headersToSave.size();
    newHeaders += headersToSave.size();
    headersToSave = new ArrayList<>();
    durationBetweenBatchSavesWatch = StopWatch.createStarted();

    return true;
  }

}
