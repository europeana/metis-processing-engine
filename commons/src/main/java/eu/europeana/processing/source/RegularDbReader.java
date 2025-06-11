package eu.europeana.processing.source;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.repository.ExecutionRecordRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import java.io.IOException;
import java.util.List;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.util.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SourceReader implementation for regular - all but not harvesting jobs.
 */
public class RegularDbReader extends AbstractDbReader<ExecutionRecord> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RegularDbReader.class);
  private ExecutionRecordRepository executionRecordRepository;

  /**
   * Creates RegularDbReader
   * @param context - Flink context
   * @param parameterTool - job parameters
   */
  public RegularDbReader(SourceReaderContext context, ParameterTool parameterTool) {
    super(context, parameterTool);
    LOGGER.info("Created RegularDbReader");
  }

  protected void createRepositories() {
    //TODO Using retry proxy is maybe not optimal strategy in this case. This source implements asynchronous interface, so
    // we could do this retries in poolNext() method by returning InputStatus.NOTHING_AVAILABLE, wait a bit and notify
    // completable future to poll source again. Or simple wait a bit in pollNext() but only once per one retry.
    // In such cases we would less block checkpointing mechanism, which should work smoothly in case of infrastructure problems
    // and potential job restarts. And when we do not block we could do more retries or longer pauses.
    executionRecordRepository = RetryableMethodExecutor.createRetryProxy(new ExecutionRecordRepository(dbConnectionProvider));
  }

  protected List<ExecutionRecord> fetchRecords() throws IOException {
    return executionRecordRepository.getByDatasetIdAndExecutionIdAndOffsetAndLimit(
        parameterTool.getRequired(JobParamName.DATASET_ID),
        parameterTool.getRequired(JobParamName.EXECUTION_ID),
        currentSplit.getOffset(), currentSplit.getLimit());
  }

}
