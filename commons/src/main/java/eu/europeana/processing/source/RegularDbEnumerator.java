package eu.europeana.processing.source;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.DataPartition;
import eu.europeana.processing.repository.ExecutionRecordRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import eu.europeana.processing.source.DbEnumeratorState.DbEnumeratorStateBuilder;
import java.io.IOException;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Enumerator implementation for regular - all but not harvesting jobs.
 */
public class RegularDbEnumerator extends
    DbEnumerator<DbEnumeratorState, DbEnumeratorState.DbEnumeratorStateBuilder<DbEnumeratorState, ?>> {

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
   * @param state enumerator state container
   * @param parameterTool parameter tool
   */
  public RegularDbEnumerator(SplitEnumeratorContext<DataPartition> context, DbEnumeratorState state,
      ParameterTool parameterTool) {
    super(context, state, parameterTool);
  }

  protected void createDbRepositories() {
    executionRecordRepository = RetryableMethodExecutor.createRetryProxy(new ExecutionRecordRepository(dbConnectionProvider));
  }

  protected long countRecordsInDb() throws IOException {
    return executionRecordRepository.countByDatasetIdAndExecutionId(
        parameterTool.getRequired(JobParamName.DATASET_ID),
        parameterTool.getRequired(JobParamName.EXECUTION_ID));
  }

  @Override
  protected DbEnumeratorStateBuilder createSnapshotBuilder() {
    return DbEnumeratorState.builder();
  }

}
