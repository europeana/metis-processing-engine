package eu.europeana.processing.source;

import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.repository.TaskInfoRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import java.io.Closeable;
import org.apache.flink.util.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class is responsible for progress counting.
 */
public class ProgressUpdater implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProgressUpdater.class);
  private final long taskId;
  private final DbConnectionProvider dbConnectionProvider;
  private final TaskInfoRepository taskInfoRepo;
  private long lastStoredFilesCount;
  private long snapshottedEmittedFilesCount = -1;

  /**
   * Creates ProgressUpdater
   *
   * @param parameterTool - all the command line parameters of the job
   * @param completedFilesCount - Number of files already completed. It is greater than 0 only if the source is restored from a
   * checkpoint.
   * @param dbConnectionProvider - db connection provider
   */
  public ProgressUpdater(DbConnectionProvider dbConnectionProvider, ParameterTool parameterTool, long completedFilesCount) {
    this.dbConnectionProvider = dbConnectionProvider;
    this.taskId = parameterTool.getLong(JobParamName.TASK_ID);
    lastStoredFilesCount = completedFilesCount;
    taskInfoRepo = RetryableMethodExecutor.createRetryProxy(new TaskInfoRepository(dbConnectionProvider));
    LOGGER.debug("Created ProgressUpdater");
  }


  /**
   * Stores count of emitted files. It is invoked during doing snapshot of enumerator state. This count that is later saved as
   * progress into DB after the checkpoint is completed.
   *
   * @param shapshotedEmittedFilesCount - number of emitted files.
   */
  public void snapshotEmittedFilesCount(long shapshotedEmittedFilesCount) {
    this.snapshottedEmittedFilesCount = shapshotedEmittedFilesCount;

  }

  /**
   * Saves previously stored count of emitted files in the DB. Invoked after the checkpoint is completed, what means that the
   * emitted files are already stored in the DB.
   */
  public void saveProgressInDB() {
    if (snapshottedEmittedFilesCount != lastStoredFilesCount) {
      TaskInfo taskInfo = new TaskInfo(taskId, 0, snapshottedEmittedFilesCount);
      //TODO The repository uses retries in case of failure, but because updating progress is not a key feature,
      // without it the task should finish its work properly. Beside that we could omit some updates of progress
      // as long as we store last progress, when the task is whole complete.
      // So we could consider more sophisticated failover mechanism with lesser impact on the execution.
      taskInfoRepo.update(taskInfo);
      lastStoredFilesCount = snapshottedEmittedFilesCount;
      LOGGER.info("Updated task progress in DB: {}", taskInfo);
    } else {
      LOGGER.info("Need not update progress for: {}", taskId);
    }
  }

  public void close() {
    dbConnectionProvider.close();
    LOGGER.debug("Closed: {}", ProgressUpdater.class.getSimpleName());
  }

}
