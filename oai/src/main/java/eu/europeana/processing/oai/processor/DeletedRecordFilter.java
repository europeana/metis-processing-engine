package eu.europeana.processing.oai.processor;

import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import org.apache.flink.api.common.functions.FilterFunction;

/**
 * Filters out deleted records.
 */
public class DeletedRecordFilter implements FilterFunction<OaiRecordHeader> {

  @Override
  public boolean filter(OaiRecordHeader value) {
    return !value.isDeleted();
  }
}
