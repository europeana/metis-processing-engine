package eu.europeana.processing.source;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.repository.ExecutionRecordRepository;
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
  private final ExecutionRecordRepository executionRecordRepository;

  /**
   * Creates RegularDbReader
   *
   * @param context - Flink context
   * @param parameterTool - job parameters
   */
  public RegularDbReader(SourceReaderContext context, ParameterTool parameterTool, ExecutionRecordRepository executionRecordRepository) {
    super(context, parameterTool);
    this.executionRecordRepository = executionRecordRepository;
    LOGGER.info("Created RegularDbReader");
  }

  protected List<ExecutionRecord> fetchRecords() throws IOException {
    return executionRecordRepository.getByDatasetIdAndExecutionIdAndOffsetAndLimit(
        parameterTool.getRequired(JobParamName.DATASET_ID),
        parameterTool.getRequired(JobParamName.EXECUTION_ID),
        currentSplit.getOffset(), currentSplit.getLimit());
  }

  @Override
  public void close() {
    super.close();
    executionRecordRepository.shutdown();
  }
}
