package eu.europeana.processing.source;

import eu.europeana.processing.exception.FlinkWorkflowException;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.repository.TaskInfoRepository;
import org.apache.flink.util.ParameterTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ProgressUpdaterTest {

  @Test
  void shouldSavePropperProgressInDbForFirstExecution() throws FlinkWorkflowException {
    TaskInfoRepository taskInfoRepository = Mockito.mock(TaskInfoRepository.class);

    try (ProgressUpdater progressUpdater = new ProgressUpdater(taskInfoRepository, ParameterTool.fromArgs(
        new String[]{
            "-" + JobParamName.TASK_ID, "12"
        }
    ), 100)) {
      progressUpdater.saveProgressInDB();

      ArgumentCaptor<TaskInfo> captor = ArgumentCaptor.forClass(TaskInfo.class);

      Mockito.verify(taskInfoRepository, Mockito.times(1)).update(captor.capture());
      TaskInfo emitted = captor.getValue();
      Assertions.assertEquals(12, emitted.taskId());
      Assertions.assertEquals(0, emitted.commitCount());
      Assertions.assertEquals(-1, emitted.writeCount());
    }
  }

  @Test
  void shouldSavePropperProgressInDbForConsecutiveExecution() throws FlinkWorkflowException {
    TaskInfoRepository taskInfoRepository = Mockito.mock(TaskInfoRepository.class);

    try (ProgressUpdater progressUpdater = new ProgressUpdater(taskInfoRepository, ParameterTool.fromArgs(
        new String[]{
            "-" + JobParamName.TASK_ID, "12"
        }
    ), 80)) {

      progressUpdater.snapshotEmittedFilesCount(100);
      progressUpdater.saveProgressInDB();

      ArgumentCaptor<TaskInfo> captor = ArgumentCaptor.forClass(TaskInfo.class);

      Mockito.verify(taskInfoRepository, Mockito.times(1)).update(captor.capture());
      TaskInfo emitted = captor.getValue();
      Assertions.assertEquals(12, emitted.taskId());
      Assertions.assertEquals(0, emitted.commitCount());
      Assertions.assertEquals(100, emitted.writeCount());
    }
  }

  @Test
  void shouldNotSavePropperProgressInDb() throws FlinkWorkflowException {
    TaskInfoRepository taskInfoRepository = Mockito.mock(TaskInfoRepository.class);

    try (ProgressUpdater progressUpdater = new ProgressUpdater(taskInfoRepository, ParameterTool.fromArgs(
        new String[]{
            "-" + JobParamName.TASK_ID, "12"
        }
    ), 100)) {
      progressUpdater.snapshotEmittedFilesCount(100);
      progressUpdater.saveProgressInDB();

      Mockito.verify(taskInfoRepository, Mockito.times(0)).update(Mockito.any());
    }
  }
}