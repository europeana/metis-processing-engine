package eu.europeana.processing.model;

import lombok.Builder;
import lombok.Data;

/**
 * <p>Main data structure used in the processing pipelines. Describes the record that will be processed
 * by the job operator or sink.
 * </p>
 *
 * <p>Consists of:
 * <li>key - direct equivalent of the database key</li>
 * <li>executionName - job name</li>
 * <li>recordsData - content of the xml file that is being processed</li>
 * </p>
 *
 */
@Data
@Builder
public class ExecutionRecord {

    private ExecutionRecordKey executionRecordKey;
    private String executionName;
    private String recordData;
}

