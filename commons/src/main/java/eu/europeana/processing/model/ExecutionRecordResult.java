package eu.europeana.processing.model;

import lombok.Builder;
import lombok.Data;

/**
 * Keeps the result of the execution of single record.
 */
@Builder
@Data
public class ExecutionRecordResult {

    private ExecutionRecord executionRecord;
    private String exception;

    public void setRecordData(String recordData) {
        executionRecord.setRecordData(recordData);
    }

    public String getRecordData() {
        return executionRecord.getRecordData();
    }

    public String getRecordId() {
        return executionRecord.getExecutionRecordKey().getRecordId();
    }

    /**
     * Prepares the instance of class {@link ExecutionRecordResult} based on the instance of {@link ExecutionRecord}
     *
     * @param executionRecord source object
     * @return ExecutionRecordResult assigned to provided {@link ExecutionRecord} instance
     */
    public static ExecutionRecordResult from(ExecutionRecord executionRecord) {
        return ExecutionRecordResult
                .builder()
                .executionRecord(executionRecord)
                .build();
    }

    /**
     * Prepares the instance of class {@link ExecutionRecordResult} based on provided arguments
     *
     * @param sourceRecord source object
     * @param taskId task identifier
     * @param stepName job name
     * @return ExecutionRecordResult containing provided values
     */
    public static ExecutionRecordResult from(ExecutionRecord sourceRecord, long taskId, String stepName) {
        ExecutionRecordResult resultRecord = ExecutionRecordResult.from(sourceRecord);
        resultRecord.getExecutionRecord().getExecutionRecordKey().setExecutionId(taskId + "");
        resultRecord.getExecutionRecord().setExecutionName(stepName);
        return resultRecord;
    }

    /**
     * Prepares the instance of class {@link ExecutionRecordResult} based on provided arguments
     *
     * @param executionRecord source object
     * @param executionId task identifier
     * @param executionName job name
     * @param recordData currently processed xml file
     * @param exception exception occurred during processing
     * @return ExecutionRecordResult containing provided values
     */
    public static ExecutionRecordResult from(
            ExecutionRecord executionRecord,
            String executionId,
            String executionName,
            String recordData,
            String exception

    ) {
        return ExecutionRecordResult
                .builder()
                .executionRecord(
                        ExecutionRecord.builder()
                                .executionRecordKey(
                                        ExecutionRecordKey.builder()
                                                .datasetId(executionRecord.getExecutionRecordKey().getDatasetId())
                                                .executionId(executionId)
                                                .recordId(executionRecord.getExecutionRecordKey().getRecordId())
                                                .build())
                                .executionName(executionName)
                                .recordData(recordData)
                                .build()
                )
                .exception(exception)
                .build();
    }
}
