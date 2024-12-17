package eu.europeana.processing.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Database key for {@link ExecutionRecord}
 */
@Data
@Builder
public class ExecutionRecordKey implements Serializable {
    private String datasetId;
    private String executionId;
    private String recordId;
}
