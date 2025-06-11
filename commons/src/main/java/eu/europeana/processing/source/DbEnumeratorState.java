package eu.europeana.processing.source;

import eu.europeana.processing.model.DataPartition;
import java.io.Serial;

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
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class DbEnumeratorState extends AbstractEnumeratorState<DataPartition> {

  @Serial
  private static final long serialVersionUID = 2;
}
