package eu.europeana.processing.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;
import org.apache.flink.api.connector.source.SourceSplit;

import java.io.Serializable;

/**
 * Class describing actual partition of data that is delivered to reader
 *
 */
@Value
@AllArgsConstructor
public class DataPartition implements AbstractPartition {

    long offset;
    long limit;
    @With
    long progress;

    @Override
    public String splitId() {
        return offset+"_"+limit;
    }

}
