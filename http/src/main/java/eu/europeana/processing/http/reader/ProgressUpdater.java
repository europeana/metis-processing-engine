package eu.europeana.processing.http.reader;

import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.http.reader.exception.HttpSourceException;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.repository.TaskInfoRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import java.io.Closeable;
import org.apache.flink.api.java.utils.ParameterTool;
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
  private int lastStoredFilesCount;
  private int shapshotedEmittedFilesCount = -1;

  /**
   * Creates ProgressUpdater
   * @param parameterTool - all the command line parameters of the job
   * @param completedFilesCount - number of files already completed. It is greater than 0 only if the
   * source is restored from a checkpoint.
   */
  public ProgressUpdater(ParameterTool parameterTool, int completedFilesCount) {
    this.taskId = parameterTool.getLong(JobParamName.TASK_ID);
    lastStoredFilesCount = completedFilesCount;
    dbConnectionProvider = new DbConnectionProvider(parameterTool);
    taskInfoRepo = RetryableMethodExecutor.createRetryProxy(new TaskInfoRepository(dbConnectionProvider));
    LOGGER.debug("Created ProgressUpdater");
  }


  /**
   * Stores count of emitted files. It is invoked during doing snapshot of enumerator state.
   * This count that is later saved as progress into DB after the checkpoint is completed.
   * @param shapshotedEmittedFilesCount - number of emitted files.
   */
  public void snapshotEmittedFilesCount(int shapshotedEmittedFilesCount) {
    this.shapshotedEmittedFilesCount = shapshotedEmittedFilesCount;

  }

  /**
   * Saves previously stored count of emitted files in the DB. Invoked after the checkpoint is completed,
   * what means that the emitted files are already stored in the DB.
   */
  public void saveProgressInDB() {
    if (shapshotedEmittedFilesCount != lastStoredFilesCount) {
      TaskInfo taskInfo = new TaskInfo(taskId, 0, shapshotedEmittedFilesCount);
      taskInfoRepo.update(taskInfo);
      lastStoredFilesCount = shapshotedEmittedFilesCount;
    }
  }

  public void close() {
    try {
      dbConnectionProvider.close();
      LOGGER.debug("Closed: {}", ProgressUpdater.class.getSimpleName());
    } catch (Exception e) {
      throw new HttpSourceException("Could not close db provider", e);
    }
  }

}
