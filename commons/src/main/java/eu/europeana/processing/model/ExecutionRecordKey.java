package eu.europeana.processing.model;

import java.io.Serial;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Database key for {@link ExecutionRecord}
 */
@Data
@Builder
public class ExecutionRecordKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1;

    private String datasetId;
    private String executionId;
    private String recordId;
}
