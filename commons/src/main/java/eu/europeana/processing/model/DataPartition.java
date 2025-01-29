package eu.europeana.processing.model;

import org.apache.flink.api.connector.source.SourceSplit;

import java.io.Serializable;

/**
 * Record describing actual partition of data that is delivered to reader
 *
 * @param offset database offset used by read to query the data
 * @param limit database limit used by reader to query the data
 */
public record DataPartition(long offset, long limit) implements SourceSplit, Serializable {

    //TODO Check if is it proper implementation.
    @Override
    public String splitId() {
        return "customSplitId";
    }
}
