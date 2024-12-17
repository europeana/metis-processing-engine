package eu.europeana.processing.model;

/**
 * Record describing current task
 *
 * @param taskId tak identifier
 * @param commitCount number of performed commit
 * @param writeCount number of records already processed
 */
public record TaskInfo(long taskId, long commitCount, long writeCount) {

}
