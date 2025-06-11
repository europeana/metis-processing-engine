package eu.europeana.processing.model;

import java.io.Serializable;
import java.util.UUID;
import org.apache.flink.api.connector.source.SourceSplit;

/**
 * Abstract interface for partitions (splits) used by abstract source implementation.
 */
public interface AbstractPartition extends SourceSplit, Serializable {

  long getProgress();

  AbstractPartition withProgress(long progress);

  AbstractPartition withEnumeratorId(UUID enumeratorId);

  long getLimit();

  UUID getEnumeratorId();

}
