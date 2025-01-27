package eu.europeana.processing.http.source;

import static java.lang.String.valueOf;

import eu.europeana.processing.http.source.extractor.ExtractionMode;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.apache.flink.api.connector.source.SourceSplit;

@Value
@Builder
public class HttpSourceSplit implements SourceSplit, Serializable {
  ExtractionMode extractionMode;
  String downloadedArchiveFile;
  int firstFileIndex;
  List<String> fileNames;

  @Override
  public String splitId() {
    return valueOf(firstFileIndex);
  }
}
