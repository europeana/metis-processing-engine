package eu.europeana.processing.oai.reader;

import eu.europeana.processing.source.DbEnumeratorState;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * State of the OAIHeadersSplitEnumerator
 */
@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OAIEnumeratorState extends DbEnumeratorState {

  @Serial
  private static final long serialVersionUID = 1;

  private boolean headersHarvested;
}
