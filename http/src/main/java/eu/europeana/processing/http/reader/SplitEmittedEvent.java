package eu.europeana.processing.http.reader;

import java.io.Serial;
import lombok.Value;
import org.apache.flink.api.connector.source.SourceEvent;

/**
 * Event meaning that given split was whole emitted by reader
 */
@Value
public class SplitEmittedEvent implements SourceEvent {
    @Serial
    private static final long serialVersionUID = 1;

    String splitId;
    int splitSize;
}
