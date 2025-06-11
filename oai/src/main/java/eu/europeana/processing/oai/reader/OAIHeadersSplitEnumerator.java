package eu.europeana.processing.oai.reader;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.DataPartition;
import eu.europeana.processing.oai.repository.OAIHeadersRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import eu.europeana.processing.source.AbstractDbEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.util.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * SplitEnumerator implementation for OAI. It is based on AbstractDbEnumerator and uses similar Db like regularDbSource, with
 * additional index column. This enumerator, generally reads and emits records from DB, but also fills this DB in background
 * thread harvesting OAI headers from OAI source using OAIBackgroundHeaderHarvester class. State of main thread emitting headers
 * from DB is often stored in checkpoint, the same as in the RegularDBEnumerator The background header harvesting state could not
 * be stored cause of limitations of current metis-harvesting implementation. So the operation is repeated whole during failover.
 * It could be potentially changed in the future. We could make more granular fail-over using resumption token storing.
 */
public class OAIHeadersSplitEnumerator extends AbstractDbEnumerator<OAIEnumeratorState> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OAIHeadersSplitEnumerator.class);
  private OAIHeadersRepository repository;
  private final OAIBackgroundHeaderHarvester backgroundHeaderHarvester;
  private boolean headersHarvested;

  /**
   * Constructor used when state restoration is not needed;
   *
   * @param context context for enumerator
   * @param parameterTool parameter tool
   * @param jobUuid - unique job id
   */
  public OAIHeadersSplitEnumerator(SplitEnumeratorContext<DataPartition> context, ParameterTool parameterTool, String jobUuid) {
    super(context, parameterTool);
    backgroundHeaderHarvester = new OAIBackgroundHeaderHarvester(this, parameterTool, jobUuid);
  }

  /**
   * Constructor used when state restoration is needed;
   *
   * @param context context for enumerator
   * @param parameterTool parameter tool
   * @param jobUuid - unique job id
   * @param state enumerator state container
   */
  public OAIHeadersSplitEnumerator(SplitEnumeratorContext<DataPartition> context, ParameterTool parameterTool,
      String jobUuid, OAIEnumeratorState state) {
    super(context, parameterTool, state);
    headersHarvested = state.isHeadersHarvested();
    backgroundHeaderHarvester = new OAIBackgroundHeaderHarvester(this, parameterTool, jobUuid);
  }

  @Override
  public void start() {
    super.start();
    if (!headersHarvested) {
      backgroundHeaderHarvester.start();
    } else {
      LOGGER.info("Headers already harvested and saved in the DB. Finished: {} of {} all records.",
          emittedRecordCount, recordsToBeProcessed);
    }
  }

  @Override
  protected void createDbRepositories() {
    repository = RetryableMethodExecutor.createRetryProxy(new OAIHeadersRepository(dbConnectionProvider));
  }

  @Override
  protected OAIEnumeratorState createState() {
    OAIEnumeratorState state = new OAIEnumeratorState();
    state.setHeadersHarvested(headersHarvested);
    return state;
  }

  @Override
  public void close() throws IOException {
    try {
      backgroundHeaderHarvester.close();
    } catch (InterruptedException e) {
      LOGGER.warn("InterruptedException during OAIBackgroundHeaderHarvester closing!", e);
      Thread.currentThread().interrupt();
    }
    super.close();
  }

  @Override
  protected long countRecordsInDb() throws IOException {
    return repository.countByDatasetIdAndExecutionId(
        parameterTool.getRequired(JobParamName.DATASET_ID),
        parameterTool.getRequired(JobParamName.TASK_ID));
  }

  /**
   * Inform the enumerator that new headers were harvested and saved in DB by background thread.
   *
   * @param allRecordInDb - number of all records in the DB, saved so far by background headers harvesting
   */
  public void notifyNewHeaderSavedInDB(int allRecordInDb) {
    context.runInCoordinatorThread(() -> {
      LOGGER.debug("Notified about new headers saved to database total count: {}, previously: {}, waiting readers: {}",
          allRecordInDb, recordsToBeProcessed, waitingReaders);
      recordsToBeProcessed = allRecordInDb;
      tryAssignWaitingReaders();
    });
  }

  /**
   * Inform enumerator that all headers were harvested in background thread.
   */
  public void notifyHeaderHarvestingFinished() {
    context.runInCoordinatorThread(() -> {
      headersHarvested = true;
      tryAssignWaitingReaders();
    });
  }

  /**
   * Inform enumerator that background header harvesting finished with exception or even error.
   *
   * @param e - exception thrown in the background thread.
   */
  public void notifyHeadersHarvestingFailed(Throwable e) {
    context.runInCoordinatorThread(() -> {
      throw new RuntimeException("Header harvesting failed!", e);
    });
  }

  @Override
  protected boolean isFinished() {
    return super.isFinished() && headersHarvested;
  }

}
