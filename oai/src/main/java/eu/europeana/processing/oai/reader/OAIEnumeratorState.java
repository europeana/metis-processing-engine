package eu.europeana.processing.oai.reader;

import eu.europeana.processing.source.DbEnumeratorState;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * State of the OAIHeadersSplitEnumerator
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class OAIEnumeratorState extends DbEnumeratorState {

  @Serial
  private static final long serialVersionUID = 1;

  private boolean headersHarvested;
}
