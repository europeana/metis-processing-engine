package eu.europeana.processing.source;

import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.model.DataPartition;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.repository.ExecutionRecordRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import java.io.Serial;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.util.ParameterTool;

public class DbSourceWithProgressHandling implements Source<ExecutionRecord, DataPartition, DbEnumeratorState> {

  @Serial
  private static final long serialVersionUID = 1;

  private final ParameterTool parameterTool;

  public DbSourceWithProgressHandling(ParameterTool parameterTool) {
    this.parameterTool = parameterTool;
  }

  @Override
  public Boundedness getBoundedness() {
    return Boundedness.BOUNDED;
  }

  @Override
  public SplitEnumerator<DataPartition, DbEnumeratorState> createEnumerator(SplitEnumeratorContext<DataPartition> enumContext) {
    return new RegularDbEnumerator(enumContext, parameterTool);
  }

  @Override
  public SplitEnumerator<DataPartition, DbEnumeratorState> restoreEnumerator(
      SplitEnumeratorContext<DataPartition> enumContext,
      DbEnumeratorState state) {
    return new RegularDbEnumerator(enumContext, parameterTool, state);
  }

  @Override
  public SourceReader<ExecutionRecord, DataPartition> createReader(SourceReaderContext readerContext) {
    return new RegularDbReader(
        readerContext,
        parameterTool,
        //TODO Using retry proxy is maybe not optimal strategy in this case. This source implements asynchronous interface, so
        // we could do this retries in poolNext() method by returning InputStatus.NOTHING_AVAILABLE, wait a bit and notify
        // completable future to poll source again. Or simple wait a bit in pollNext() but only once per one retry.
        // In such cases we would less block checkpointing mechanism, which should work smoothly in case of infrastructure problems
        // and potential job restarts. And when we do not block we could do more retries or longer pauses.
        RetryableMethodExecutor.createRetryProxy(new ExecutionRecordRepository(new DbConnectionProvider(parameterTool))));
  }

  @Override
  public SimpleVersionedSerializer<DataPartition> getSplitSerializer() {
    return new ObjectStreamVersionedSerializer<>();
  }

  @Override
  public SimpleVersionedSerializer<DbEnumeratorState> getEnumeratorCheckpointSerializer() {
    return new ObjectStreamVersionedSerializer<>();
  }

}
