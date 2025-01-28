package eu.europeana.processing.source;

import eu.europeana.processing.model.DataPartition;
import java.io.Serial;
import lombok.Value;
import org.apache.flink.api.connector.source.SourceEvent;

/**
 * Event meaning that given split was completed by reader and all the record are saved in the DB.
 */
@Value
public class SplitCompletedEvent implements SourceEvent {

  @Serial
  private static final long serialVersionUID = 1;

  long checkpointId;
  DataPartition split;
  int completedRecords;

}
