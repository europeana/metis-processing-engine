package eu.europeana.processing.http.reader;

import static java.lang.String.valueOf;

import eu.europeana.processing.http.reader.extractor.ExtractionMode;
import eu.europeana.processing.model.AbstractPartition;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.apache.flink.api.connector.source.SourceSplit;


/**
 * Implementation of SourceSplit for HttpSource containing chunk of names of the files to be extracted.
 */
@Value
@Builder
public class HttpSourceSplit implements AbstractPartition {
  @Serial
  private static final long serialVersionUID = 1;

  ExtractionMode extractionMode;
  String downloadedArchiveFile;
  long firstFileIndex;
  ArrayList<String> fileNames;

  @With
  long progress;

  @Override
  public String splitId() {
    return valueOf(firstFileIndex);
  }

  @Override
  public long getProgress() {
    return progress;
  }

  @Override
  public long getLimit() {
    return fileNames.size();
  }
}
