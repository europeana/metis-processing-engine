package eu.europeana.processing.source;

import eu.europeana.processing.model.DataPartition;
import eu.europeana.processing.model.ExecutionRecord;
import java.io.Serial;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.io.SimpleVersionedSerializer;

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
    public SplitEnumerator<DataPartition, DbEnumeratorState> createEnumerator(SplitEnumeratorContext<DataPartition> enumContext) throws Exception {
        return new DbEnumerator(enumContext, parameterTool);
    }

    @Override
    public SplitEnumerator<DataPartition, DbEnumeratorState> restoreEnumerator(
        SplitEnumeratorContext<DataPartition> enumContext,
        DbEnumeratorState state) throws Exception {
        return new DbEnumerator(enumContext, state, parameterTool);
    }

    @Override
    public SourceReader<ExecutionRecord, DataPartition> createReader(SourceReaderContext readerContext) throws Exception {
        return new DbReaderWithProgressHandling(readerContext, parameterTool);
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
