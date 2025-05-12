package eu.europeana.processing.http.reader;

import eu.europeana.processing.http.reader.extractor.ExtractionMode;
import eu.europeana.processing.source.AbstractEnumeratorState;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Stores a state of HttpEnumerator
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class HttpEnumeratorState extends AbstractEnumeratorState<HttpSourceSplit> {
  @Serial
  private static final long serialVersionUID = 1;

  private String downloadedFile;
  private ExtractionMode extractionMode;
}
