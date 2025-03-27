package eu.europeana.processing.oai.reader;

import static eu.europeana.processing.job.JobParamName.METADATA_PREFIX;
import static eu.europeana.processing.job.JobParamName.OAI_REPOSITORY_URL;
import static eu.europeana.processing.job.JobParamName.SET_SPEC;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.HarvestingIterator;
import eu.europeana.metis.harvesting.ReportingIteration.IterationResult;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.oai.repository.OAIHeadersRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.flink.api.java.utils.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Harvest records from OAI source in background thread and saves them in the DB table.
 * It also notifies enumerator about progress, completion and failure, so it could react.
 */
public class OAIBackgroundHeaderHarvester {

  private static final int DEFAULT_RETRIES = 3;
  private static final int SLEEP_TIME = 5000;
  private static final long PROGRESS_INTERVAL = 10;
  private static final Logger LOGGER = LoggerFactory.getLogger(OAIBackgroundHeaderHarvester.class);
  private final String dataset;
  private final String execution;
  private final ParameterTool parameterTool;
  private final OAIHeadersSplitEnumerator enumerator;
  private int allRecordsInDb;
  private ExecutorService backgroudExecutor;
  private Future<?> future;
  private int harvestedHeaders;
  private int newHeaders;
  private StopWatch progressWatch;
  private OaiHarvest oaiHarvest;
  private DbConnectionProvider dbConnectionProvider;
  private OAIHeadersRepository repository;

  /**
   * Creates OAIBackgroundHeaderHarvester.
   *
   * @param enumerator - enumerator instance which is notified about harvesting progress, completion and failure.
   * @param parameterTool - job parameters
   */
  public OAIBackgroundHeaderHarvester(OAIHeadersSplitEnumerator enumerator, ParameterTool parameterTool) {
    this.enumerator = enumerator;
    this.parameterTool = parameterTool;
    this.dataset = parameterTool.getRequired(JobParamName.DATASET_ID);
    this.execution = parameterTool.getRequired(JobParamName.TASK_ID);
  }

  /**
   * Starts harvesting - in background, method is not blocking
   */
  public void start() {
    LOGGER.info("Starting OAIBackgroundHeaderHarvester - creating repositories");
    dbConnectionProvider = new DbConnectionProvider(parameterTool);
    repository = RetryableMethodExecutor.createRetryProxy(new OAIHeadersRepository(dbConnectionProvider));
    LOGGER.info("OAIBackgroundHeaderHarvester - starting background thread");
    backgroudExecutor = Executors.newFixedThreadPool(1, r -> new Thread(r, "OAI-harvesting-" + dataset));
    future = backgroudExecutor.submit(this::execute);
  }

  /**
   * Stops background harvesting thread. And wait for its stopping.
   * @throws InterruptedException - if the current thread was interrupted during waiting for background operation stopping.
   */
  public void close() throws InterruptedException {
    try {
      LOGGER.info("Closing OAIBackgroundHeaderHarvester...");
      backgroudExecutor.shutdownNow();
      //We could block on get without timeout, cause Flink will kill process if it would take too long.
      future.get();
      if (dbConnectionProvider != null) {
        dbConnectionProvider.close();
      }
      LOGGER.info("Closed OAIBackgroundHeaderHarvester");
    } catch (ExecutionException e) {
      //Should not be caught during normal work
      throw new RuntimeException("Unexpected error during closing background harvester!", e);
    }
  }

  private void execute() {
    try{
      progressWatch = StopWatch.createStarted();
      countHeadersAlreadySavedInDB();
      harvestHeaders();
      if(Thread.currentThread().isInterrupted()){
        return;
      }
      enumerator.notifyHeaderHarvestingFinished();
      logProgress();
      LOGGER.info("Finished background headers harvesting");
    } catch (@SuppressWarnings("java:S1181") Throwable e) { //We catch errors because we want to instantly notify enumerator
      LOGGER.warn("Error during harvesting background headers", e);
      enumerator.notifyHeadersHarvestingFailed(e);
    }
  }

  private void countHeadersAlreadySavedInDB() throws IOException {
    allRecordsInDb = (int) repository.countByDatasetIdAndExecutionId(dataset, execution);
    LOGGER.info("Counted: {} OAI headers already in DB.", allRecordsInDb);
    if(allRecordsInDb > 0) {
      //It happens when task is restarted (for example from a checkpoint) and there are records already in DB.
      enumerator.notifyNewHeaderSavedInDB(allRecordsInDb);
    }
  }

  private void harvestHeaders()
      throws IOException, HarvesterException {
    oaiHarvest = new OaiHarvest(
        parameterTool.getRequired(OAI_REPOSITORY_URL),
        parameterTool.getRequired(METADATA_PREFIX),
        parameterTool.getRequired(SET_SPEC));

    LOGGER.info("Starting harvesting of: {}", oaiHarvest);
    OaiHarvester harvester = HarvesterFactory.createOaiHarvester(null, DEFAULT_RETRIES, SLEEP_TIME);

    try (HarvestingIterator<OaiRecordHeader, OaiRecordHeader> headerIterator = harvester.harvestRecordHeaders(oaiHarvest)) {
      headerIterator.forEach(oaiHeader -> {
        if (Thread.currentThread().isInterrupted()) {
          return IterationResult.TERMINATE;
        }
        saveHeaderInDb(oaiHeader);
        return IterationResult.CONTINUE;
      });
    }
  }

  private void saveHeaderInDb(OaiRecordHeader oaiHeader) throws IOException {
    //TODO this method could return false even if record was saved but the method ended with exception because of
    //other reasons, for example the result could not be read cause of network issues.
    //We should add some mitigation for example doing get after retry to double check.
    if (repository.save(dataset, execution, oaiHeader, allRecordsInDb)) {
      newHeaders++;
      allRecordsInDb++;
      enumerator.notifyNewHeaderSavedInDB(allRecordsInDb);
    }
    harvestedHeaders++;
    logProgressNotMoreOftenThanEvery10Seconds();
  }
  
  private void logProgressNotMoreOftenThanEvery10Seconds() {
    if (progressWatch.getTime(TimeUnit.SECONDS) >= PROGRESS_INTERVAL) {
      logProgress();
      progressWatch.reset();
      progressWatch.start();
    }
  }

  private void logProgress() {
    LOGGER.info("Harvested {} headers, saved: {} new headers, all records in DB: {} for: {}",
        harvestedHeaders, newHeaders, allRecordsInDb, oaiHarvest);
  }
}
