package eu.europeana.processing.source;

import eu.europeana.processing.model.DataPartition;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * State container for enumerator
 */
@Data
@Builder
public class DbEnumeratorState implements Serializable {
  private long recordsToBeProcessed;
  private long allPartitionCount;
  private long startedPartitionCount;
  private long finishedRecordCount;
  private long commitCount;
  private List<DataPartition> incompletePartitions;

}
