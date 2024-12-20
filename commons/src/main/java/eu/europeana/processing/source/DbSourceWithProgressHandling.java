package eu.europeana.processing.source;

import eu.europeana.processing.model.DataPartition;
import eu.europeana.processing.model.ExecutionRecord;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.io.SimpleVersionedSerializer;

public class DbSourceWithProgressHandling implements Source<ExecutionRecord, DataPartition, DbEnumeratorState> {

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
        return new DbEnumerator(enumContext, null, parameterTool);
    }

    @Override
    public SplitEnumerator<DataPartition, DbEnumeratorState> restoreEnumerator(SplitEnumeratorContext<DataPartition> enumContext, DbEnumeratorState state) throws Exception {
        return new DbEnumerator(enumContext, state, parameterTool);
    }

    @Override
    public SourceReader<ExecutionRecord, DataPartition> createReader(SourceReaderContext readerContext) throws Exception {
        return new DbReaderWithProgressHandling(readerContext, parameterTool);
    }

    @Override
    public SimpleVersionedSerializer<DataPartition> getSplitSerializer() {
        return new SimpleVersionedSerializer<>() {
            @Override
            public int getVersion() {
                return 0;
            }

            @Override
            public byte[] serialize(DataPartition obj) throws IOException {
                var boas = new ByteArrayOutputStream();
                var oos = new ObjectOutputStream(boas);
                oos.writeObject(obj);

                return boas.toByteArray();
            }

            @Override
            public DataPartition deserialize(int version, byte[] serialized) throws IOException {
                var bais = new ByteArrayInputStream(serialized);
                var ois = new ObjectInputStream(bais);
                DataPartition res = null;
                try {
                    res = (DataPartition) ois.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                ois.close();
                bais.close();
                return res;
            }
        };
    }

    @Override
    public SimpleVersionedSerializer<DbEnumeratorState> getEnumeratorCheckpointSerializer() {
        return new SimpleVersionedSerializer<>() {
            @Override
            public int getVersion() {
                return 0;
            }

            @Override
            public byte[] serialize(DbEnumeratorState obj) throws IOException {
                var boas = new ByteArrayOutputStream();
                var oos = new ObjectOutputStream(boas);
                oos.writeObject(obj);

                return boas.toByteArray();
            }

            @Override
            public DbEnumeratorState deserialize(int version, byte[] serialized) throws IOException {
                var bais = new ByteArrayInputStream(serialized);
                var ois = new ObjectInputStream(bais);
                DbEnumeratorState res = null;
                try {
                    res = (DbEnumeratorState) ois.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                ois.close();
                bais.close();
                return res;
            }
        };
    }
}
