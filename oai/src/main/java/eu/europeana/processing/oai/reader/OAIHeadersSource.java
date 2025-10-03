package eu.europeana.processing.oai.reader;

import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.model.DataPartition;
import eu.europeana.processing.oai.repository.OAIHeadersRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import eu.europeana.processing.source.ObjectStreamVersionedSerializer;
import java.io.Serial;
import java.util.UUID;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.util.ParameterTool;
import org.apache.flink.core.io.SimpleVersionedSerializer;

/**
 * Source reading OAI headers from OAI sources.
 */
public class OAIHeadersSource implements Source<OaiRecordHeader, DataPartition, OAIEnumeratorState>,
    ResultTypeQueryable<OaiRecordHeader> {

  @Serial
  private static final long serialVersionUID = 1;

  private final ParameterTool parameterTool;

  private final String jobUuid = UUID.randomUUID().toString();

  /**
   * Creates OAIHeaderSource
   *
   * @param parameterTool - job parameters
   */
  public OAIHeadersSource(ParameterTool parameterTool) {
    this.parameterTool = parameterTool;
  }

  @Override
  public Boundedness getBoundedness() {
    return Boundedness.BOUNDED;
  }

  @Override
  public SplitEnumerator<DataPartition, OAIEnumeratorState> createEnumerator(SplitEnumeratorContext<DataPartition> enumContext) {
    return new OAIHeadersSplitEnumerator(enumContext, parameterTool, jobUuid);
  }

  @Override
  public SplitEnumerator<DataPartition, OAIEnumeratorState> restoreEnumerator(SplitEnumeratorContext<DataPartition> enumContext,
      OAIEnumeratorState state) {
    return new OAIHeadersSplitEnumerator(enumContext, parameterTool, jobUuid, state);
  }

  @Override
  public SourceReader<OaiRecordHeader, DataPartition> createReader(SourceReaderContext readerContext) {
    return new OAIHeadersReader(
        readerContext,
        parameterTool,
        RetryableMethodExecutor.createRetryProxy(new OAIHeadersRepository(new DbConnectionProvider(parameterTool))));
  }

  @Override
  public SimpleVersionedSerializer<DataPartition> getSplitSerializer() {
    return new ObjectStreamVersionedSerializer<>();
  }

  @Override
  public SimpleVersionedSerializer<OAIEnumeratorState> getEnumeratorCheckpointSerializer() {
    return new ObjectStreamVersionedSerializer<>();
  }

  @Override
  public TypeInformation<OaiRecordHeader> getProducedType() {
    return TypeInformation.of(OaiRecordHeader.class);
  }

}
