package eu.europeana.processing.oai.processor;

import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import java.io.Serial;
import org.apache.flink.api.common.functions.FilterFunction;

/**
 * Filters out deleted records.
 */
public class DeletedRecordFilter implements FilterFunction<OaiRecordHeader> {

  @Serial
  private static final long serialVersionUID = 1;

  @Override
  public boolean filter(OaiRecordHeader value) {
    return !value.isDeleted();
  }
}
