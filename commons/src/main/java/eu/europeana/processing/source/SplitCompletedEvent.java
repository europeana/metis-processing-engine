package eu.europeana.processing.source;

import java.io.Serial;
import java.util.UUID;
import lombok.Value;
import org.apache.flink.api.connector.source.SourceEvent;

/**
 * Event meaning that given split was completed by reader and all the record are saved in the DB.
 */
@Value
public class SplitCompletedEvent implements SourceEvent {

  @Serial
  private static final long serialVersionUID = 1;

  String splitId;

  long completedCount;
  UUID enumeratorId;
}
