package eu.europeana.processing.oai.reader;

import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.oai.repository.OAIHeadersRepository;
import eu.europeana.processing.source.AbstractDbReader;
import java.io.IOException;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.util.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SourceReader implementation for OAI sources. It reads headers from the DB and emits them.
 */
public class OAIHeadersReader extends AbstractDbReader<OaiRecordHeader> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OAIHeadersReader.class);

  private final OAIHeadersRepository repository;

  /**
   * Creates OAIHeadersReader
   *
   * @param context - Flink context
   * @param parameterTool - job parameters
   */
  public OAIHeadersReader(SourceReaderContext context, ParameterTool parameterTool, OAIHeadersRepository oaiHeadersRepository) {
    super(context, parameterTool);
    this.repository = oaiHeadersRepository;
    LOGGER.info("Created OAIHeadersReader");
  }

  protected List<OaiRecordHeader> fetchRecords() throws IOException {
    return repository.getByDatasetIdAndExecutionIdAndOffsetAndLimit(
        parameterTool.getRequired(JobParamName.DATASET_ID),
        parameterTool.getRequired(JobParamName.TASK_ID),
        currentSplit.getOffset(),
        currentSplit.getLimit());
  }

}
