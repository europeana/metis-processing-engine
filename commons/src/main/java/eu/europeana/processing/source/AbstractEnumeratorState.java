package eu.europeana.processing.source;

import eu.europeana.processing.model.AbstractPartition;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * State container for enumerator
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@SuperBuilder
public abstract class AbstractEnumeratorState<P extends AbstractPartition> implements Serializable {

  @Serial
  private static final long serialVersionUID = 2;

  private long recordsToBeProcessed;
  private long startedRecordsCount;
  private long finishedRecordCount;
  private List<P> incompletePartitions;

}
