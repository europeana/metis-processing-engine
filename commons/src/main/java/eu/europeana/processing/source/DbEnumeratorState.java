package eu.europeana.processing.source;

import eu.europeana.processing.model.DataPartition;
import java.io.Serial;

import java.io.Serializable;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * State container for enumerator
 */
@Getter
@SuperBuilder
@ToString
@EqualsAndHashCode
public class DbEnumeratorState implements Serializable {

  @Serial
  private static final long serialVersionUID = 2;

  private long recordsToBeProcessed;
  private long startedRecordsCount;
  private long finishedRecordCount;
  private long commitCount;
  private List<DataPartition> incompletePartitions;

}
