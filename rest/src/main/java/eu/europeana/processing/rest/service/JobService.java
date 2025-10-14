package eu.europeana.processing.rest.service;


import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.rest.dto.JobDetailsDto;
import eu.europeana.processing.rest.exception.ApplicationException;
import eu.europeana.processing.rest.repository.TaskInfoRepository;
import eu.europeana.processing.rest.service.k8s.K8sObjectRetriever;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JobService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

  private final TaskInfoRepository taskInfoRepository;
  private final K8sObjectRetriever k8sObjectRetriever;

  public JobService(TaskInfoRepository taskInfoRepository, K8sObjectRetriever k8sObjectRetriever) {
    this.taskInfoRepository = taskInfoRepository;
    this.k8sObjectRetriever = k8sObjectRetriever;
  }


  public JobDetailsDto getTaskInfo(String taskId, String taskName) throws ApplicationException {
    try {
      TaskInfo taskInfo = taskInfoRepository.findById(taskId).orElseThrow();
      taskInfo.setTaskName(taskName);
      V1Job taskJob = k8sObjectRetriever.retrieveJob(taskInfo);

      int successes = 0;
      int failures = 0;
      if (taskJob.getStatus().getSucceeded() != null) {
        successes = taskJob.getStatus().getSucceeded();
      }
      if (taskJob.getStatus().getFailed() != null) {
        failures = taskJob.getStatus().getFailed();
      }

      return JobDetailsDto.fromTaskInfo(taskInfo, successes, failures);

    } catch (ApiException e) {
      LOGGER.error("Enable to retrieve job status for task with id '{}'", taskId, e);
      throw new ApplicationException(e);
    }
  }
}
