package eu.europeana.processing.model;

import java.io.Serializable;
import org.apache.flink.api.connector.source.SourceSplit;

public interface AbstractPartition extends SourceSplit, Serializable {

  long getProgress();

  AbstractPartition withProgress(long progress);

  long getLimit();
}
