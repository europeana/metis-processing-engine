package eu.europeana.processing.model;

/**
 * DTO describing flink job details
 *
 * @param taskId
 * @param commitCount
 * @param writeCount
 * @param succeeded
 * @param failed
 */
public record JobDetailsDto(
    long taskId,
    long commitCount,
    long writeCount,
    int succeeded,
    int failed
    ) {


  /**
   * Converter from {@link TaskInfo} to {@link JobDetailsDto}
   * @param taskInfo task definition
   * @param succeeded k8s job successes
   * @param failed k8s job failures
   * @return {@link JobDetailsDto}
   */
  public static JobDetailsDto fromTaskInfo(TaskInfo taskInfo, Integer succeeded, Integer failed) {


    return new JobDetailsDto(
        taskInfo.getTaskId(),
        taskInfo.getCommitCount(),
        taskInfo.getWriteCount(),
        succeeded,
        failed);
  }
}
