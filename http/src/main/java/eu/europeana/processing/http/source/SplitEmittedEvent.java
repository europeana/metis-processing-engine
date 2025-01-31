package eu.europeana.processing.http.source;

import lombok.Value;
import org.apache.flink.api.connector.source.SourceEvent;

/**
 * Event meaning that given split was whole emitted by reader
 */
@Value
public class SplitEmittedEvent implements SourceEvent {
    String splitId;
    int splitSize;
}
