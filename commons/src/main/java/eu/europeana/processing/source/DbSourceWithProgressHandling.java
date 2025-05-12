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
import org.apache.flink.util.ParameterTool;
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
    public SplitEnumerator<DataPartition, DbEnumeratorState> createEnumerator(SplitEnumeratorContext<DataPartition> enumContext) {
        return new RegularDbEnumerator(enumContext, parameterTool, null);
    }

    @Override
    public SplitEnumerator<DataPartition, DbEnumeratorState> restoreEnumerator(
        SplitEnumeratorContext<DataPartition> enumContext,
        DbEnumeratorState state) {
        return new RegularDbEnumerator(enumContext, parameterTool, state);
    }

    @Override
    public SourceReader<ExecutionRecord, DataPartition> createReader(SourceReaderContext readerContext) {
        return new RegularDbReader(readerContext, parameterTool);
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
