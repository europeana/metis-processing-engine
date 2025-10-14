package eu.europeana.processing.rest.service;

import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.rest.dto.JobSubmissionDto;
import eu.europeana.processing.rest.exception.ApplicationException;
import eu.europeana.processing.rest.repository.TaskInfoRepository;
import eu.europeana.processing.rest.service.k8s.FlinkJobSubmitter;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Submits flink job to the specified cluster
 */
@Service
public class JobSubmissionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobSubmissionService.class);
  private final TaskInfoRepository taskInfoRepository;
  private final FlinkJobSubmitter flinkJobSubmitter;

  /**
   * Service constructor
   *
   * @param taskInfoRepository {@link TaskInfoRepository}
   * @param flinkJobSubmitter {@link FlinkJobSubmitter}
   */
  public JobSubmissionService(TaskInfoRepository taskInfoRepository, FlinkJobSubmitter flinkJobSubmitter) {
    this.taskInfoRepository = taskInfoRepository;
    this.flinkJobSubmitter = flinkJobSubmitter;
  }

  /**
   * Submits flink job to the specified cluster
   *
   * @param jobSubmissionDto {@link JobSubmissionDto}
   * @return {@link TaskInfo}
   * @throws ApplicationException exception
   */
  public TaskInfo submitJob(JobSubmissionDto jobSubmissionDto) throws ApplicationException {
    LOGGER.info("Submitting Job; {}", jobSubmissionDto);
    TaskInfo newTaskDefinition = createNewTaskDefinition(jobSubmissionDto);
    taskInfoRepository.save(newTaskDefinition);
    flinkJobSubmitter.submit(newTaskDefinition);
    LOGGER.info("Submitted Job for {}", newTaskDefinition);
    return newTaskDefinition;
  }


  private TaskInfo createNewTaskDefinition(JobSubmissionDto jobSubmissionDto) {
    final Random taskIdGenerator = new Random();
    return TaskInfo.builder()
                   .taskId(taskIdGenerator.nextLong())
                   .parameters(jobSubmissionDto.parameters())
                   .taskName(jobSubmissionDto.jobName())
                   .build();
  }

}
