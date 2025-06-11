package eu.europeana.processing.model;

import java.io.Serial;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

/**
 * Class describing actual partition of data that is delivered to reader
 *
 */
@Value
@AllArgsConstructor
public class DataPartition implements AbstractPartition {

    @Serial
    private static final long serialVersionUID = 1;

    long offset;
    long limit;
    @With
    long progress;
    @With
    UUID enumeratorId;

    @Override
    public String splitId() {
        return offset+"_"+limit;
    }

}
