package eu.europeana.processing.http.reader;

import eu.europeana.processing.http.reader.extractor.ExtractionMode;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Stores a state of HttpEnumerator
 */
@Data
@Builder
public class HttpEnumeratorState implements Serializable {
  @Serial
  private static final long serialVersionUID = 1;

  private String downloadedFile;
  private ExtractionMode extractionMode;
  private int startedFilesCount;
  private int completedFilesCount;
  private List<HttpSourceSplit> returnedPartitions;
}
