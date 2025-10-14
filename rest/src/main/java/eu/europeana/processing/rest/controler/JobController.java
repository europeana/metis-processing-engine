package eu.europeana.processing.rest.controler;

import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.rest.dto.JobDetailsDto;
import eu.europeana.processing.rest.dto.JobSubmissionDto;
import eu.europeana.processing.rest.exception.ApplicationException;
import eu.europeana.processing.rest.service.JobService;
import eu.europeana.processing.rest.service.JobSubmissionService;
import eu.europeana.processing.rest.validation.SubmittedJobParamsValidator;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsible for job related requests, maily job submission and job status checks.
 */
@RestController
@RequestMapping("jobs")
public class JobController {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobController.class);

  private final JobSubmissionService jobSubmissionService;
  private final JobService jobService;
  private final SubmittedJobParamsValidator submittedJobParamsValidator;


  /**
   * Controller constructor
   * @param jobSubmissionService service responsible for job submission
   * @param jobService  service responsible to job related actions like jak status check
   * @param submittedJobParamsValidator service responsible for validating submission requests
   */
  public JobController(JobSubmissionService jobSubmissionService,
      JobService jobService, SubmittedJobParamsValidator submittedJobParamsValidator) {
    this.jobSubmissionService = jobSubmissionService;
    this.jobService = jobService;
    this.submittedJobParamsValidator = submittedJobParamsValidator;
  }

  /**
   * Endpoint for submitting tasks (jobs)
   * @param jobSubmissionDto {@link JobSubmissionDto}
   * @return {@link JobDetailsDto}
   * @throws ApplicationException exception
   */
  @PostMapping
  public ResponseEntity<JobDetailsDto> submitNewJob(@RequestBody JobSubmissionDto jobSubmissionDto) throws ApplicationException {
    LOGGER.info("Submitting New Job");
    if (submittedJobParamsValidator.validate(jobSubmissionDto)) {
      TaskInfo taskInfo = jobSubmissionService.submitJob(jobSubmissionDto);
      return ResponseEntity.status(HttpStatus.CREATED).body(JobDetailsDto.fromTaskInfo(taskInfo, 0, 0));
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  /**
   * Endpoint for getting job details.
   *
   * @param taskName task name
   * @param jobId job id
   * @return {@link JobDetailsDto}
   * @throws ApplicationException in case of issues while reading job details
   */
  @GetMapping(value = "/{task-name}/{jobId}", produces = "application/json")
  @Operation(summary = "Get job details", description = "Returns job details for given ID")
  public JobDetailsDto getJobDetails(@PathVariable("task-name") String taskName, @PathVariable("jobId") String jobId)
      throws ApplicationException {

    return jobService.getTaskInfo(jobId, taskName);
  }
}
