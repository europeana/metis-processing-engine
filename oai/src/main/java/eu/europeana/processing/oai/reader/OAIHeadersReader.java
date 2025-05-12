package eu.europeana.processing.oai.reader;

import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.oai.repository.OAIHeadersRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import eu.europeana.processing.source.DbReaderWithProgressHandling;
import java.io.IOException;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.util.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SourceReader implementation for OAI source. It read headers from the DB and emits them.
 */
public class OAIHeadersReader extends DbReaderWithProgressHandling<OaiRecordHeader> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OAIHeadersReader.class);

  private OAIHeadersRepository repository;

  /**
   * Creates OAIHeadersReader
   * @param context - Flink context
   * @param parameterTool - job parameters
   */
  public OAIHeadersReader(SourceReaderContext context, ParameterTool parameterTool) {
    super(context,parameterTool);
    LOGGER.info("Created OAIHeadersReader");
  }

  protected void createRepositories() {
    repository = RetryableMethodExecutor.createRetryProxy(new OAIHeadersRepository(dbConnectionProvider));
  }

  protected List<OaiRecordHeader> fetchRecords() throws IOException {
    return repository.getByDatasetIdAndExecutionIdAndOffsetAndLimit(
        parameterTool.getRequired(JobParamName.DATASET_ID),
        parameterTool.getRequired(JobParamName.TASK_ID),
        currentSplit.getOffset(),
        currentSplit.getLimit());
  }

}
