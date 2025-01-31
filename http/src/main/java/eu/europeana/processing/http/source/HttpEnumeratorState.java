package eu.europeana.processing.http.source;

import eu.europeana.processing.http.source.extractor.ExtractionMode;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HttpEnumeratorState implements Serializable {
  private String downloadedFile;
  private ExtractionMode extractionMode;
  private int startedFilesCount;
  private int completedFilesCount;
  private List<HttpSourceSplit> returnedPartitions;
}
