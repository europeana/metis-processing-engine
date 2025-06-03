package eu.europeana.processing.source;

import java.io.Serial;
import java.util.UUID;
import lombok.Value;
import org.apache.flink.api.connector.source.SourceEvent;

/**
 * Event with progress snapshot was created on a reader - number of record emitted by reader to execution.
 * The event does not mean that records are already stored in DB, but contains chekpointId, so could be
 * held and used when given checkpoint is completed.
 */
@Value
public class ProgressSnapshotEvent implements SourceEvent {

  @Serial
  private static final long serialVersionUID = 1;
  long checkpointId;
  String splitId;
  long progress;
  UUID enumeratorId;
}
