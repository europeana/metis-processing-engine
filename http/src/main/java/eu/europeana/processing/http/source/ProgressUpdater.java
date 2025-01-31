package eu.europeana.processing.http.source;

import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.http.source.exception.HttpSourceException;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpEnumerator.class);
  private final long taskId;
  private final DbConnectionProvider dbConnectionProvider;
  private final TaskInfoRepository taskInfoRepo;
  private int lastStoredFilesCount;
  private int shapshotedEmittedFilesCount = -1;

  public ProgressUpdater(ParameterTool parameterTool, int completedFilesCount) {
    this.taskId = parameterTool.getLong(JobParamName.TASK_ID);
    lastStoredFilesCount = completedFilesCount;
    dbConnectionProvider = new DbConnectionProvider(parameterTool);
    taskInfoRepo = RetryableMethodExecutor.createRetryProxy(new TaskInfoRepository(dbConnectionProvider));
    LOGGER.info("Created DB repository");
  }


  public void snapshotEmittedFilesCount(int shapshotedEmittedFilesCount) {
    this.shapshotedEmittedFilesCount = shapshotedEmittedFilesCount;

  }

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
      LOGGER.info("Closed: {}", ProgressUpdater.class.getSimpleName());
    } catch (Exception e) {
      throw new HttpSourceException("Could not close db provider", e);
    }
  }

}
