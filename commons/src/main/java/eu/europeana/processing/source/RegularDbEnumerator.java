package eu.europeana.processing.source;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.DataPartition;
import eu.europeana.processing.repository.ExecutionRecordRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import java.io.IOException;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.util.ParameterTool;


/**
 * Enumerator implementation for regular - all but not harvesting jobs.
 */
public class RegularDbEnumerator extends AbstractDbEnumerator<DbEnumeratorState> {

  private ExecutionRecordRepository executionRecordRepository;

  /**
   * Constructor used when state restoration is not needed;
   *
   * @param context context for enumerator
   * @param parameterTool parameter tool
   */
  public RegularDbEnumerator(SplitEnumeratorContext<DataPartition> context, ParameterTool parameterTool) {
    super(context, parameterTool);
  }

  /**
   * Constructor used when state restoration is needed;
   *
   * @param context context for enumerator
   * @param parameterTool parameter tool
   * @param state enumerator state container
   */
  public RegularDbEnumerator(SplitEnumeratorContext<DataPartition> context,
      ParameterTool parameterTool, DbEnumeratorState state) {
    super(context, parameterTool, state);
  }

  protected void createDbRepositories() {
    executionRecordRepository = RetryableMethodExecutor.createRetryProxy(new ExecutionRecordRepository(dbConnectionProvider));
  }


  @Override
  protected DbEnumeratorState createState() {
    return new DbEnumeratorState();
  }

  protected long countRecordsInDb() throws IOException {
    return executionRecordRepository.countByDatasetIdAndExecutionId(
        parameterTool.getRequired(JobParamName.DATASET_ID),
        parameterTool.getRequired(JobParamName.EXECUTION_ID));
  }


}
