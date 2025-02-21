package eu.europeana.cloud.flink.client;

import eu.europeana.cloud.flink.client.entities.*;

import java.util.*;

import eu.europeana.cloud.flink.client.entities.JobConfigResponse.ExecutionConfig;
import eu.europeana.cloud.flink.client.entities.JobConfigResponse.ExecutionConfig.UserConfig;
import eu.europeana.cloud.flink.client.entities.JobOverviewResponse.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

public class JobExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutor.class);
  private static final String STATE_FINISHED = "FINISHED";
  private static final String STATE_CANCELED = "CANCELED";
  private static final String STATE_FAILED = "FAILED";
  private static final Set<String> END_STATES = Set.of(STATE_FINISHED, STATE_FAILED, STATE_CANCELED);
  public static final int MAX_RETRIES = 30;
  public static final long SLEEP_BETWEEN_RETRIES = 200L;
  private static final int CONNECTION_TIMEOUT_FOR_SUBMIT_REQUEST = 60_000;
  private static final int READ_TIMEOUT_FOR_SUBMIT_REQUEST = 60_000;
  private static final int CONNECTION_TIMEOUT_FOR_PROGRESS_REQUEST = 10_000;
  private static final int READ_TIMEOUT_FOR_PROGRESS_REQUEST = 10_000;
  private static final long WAIT_BEFORE_PROGRESS_CHECK_IN_MILLIS = 200;
  private static final long PROGRESS_PRINT_INTERVAL = 5;
  private static final int RECENT_TASK_THRESHOLD = 10 * 60 * 1000;

  private final String jarId;
  private final RestTemplate submitRestTemplate;
  private final RestTemplate progressRestTemplate;
  private final String serverUrl;
  private final HttpHeaders httpHeader;

  public JobExecutor(Properties serverConfiguration) {
    this(serverConfiguration.getProperty("job.manager.url"),
        serverConfiguration.getProperty("job.manager.user"),
        serverConfiguration.getProperty("job.manager.password"),
        serverConfiguration.getProperty("jar.id"));
  }

  public JobExecutor(AbstractEnvironment serverConfiguration) {
    this(serverConfiguration.getProperty("job.manager.url"),
        serverConfiguration.getProperty("job.manager.user"),
        serverConfiguration.getProperty("job.manager.password"),
        serverConfiguration.getProperty("jar.id"));
  }

  public JobExecutor(String serverUrl, String user, String password, String jarId) {
    this.serverUrl = serverUrl;
    httpHeader = new HttpHeaders();
    httpHeader.setBasicAuth(user, password);
    //We create different templates for submit job request which could take dozens of seconds, and we need bigger timeout,
    // than for progress request, which is fast, and we want to have fast reaction to measure test time accurately.
    submitRestTemplate = createSubmitRestTemplate();
    progressRestTemplate = createProgressRestTemplate();
    this.jarId = jarId;
  }

  public void execute(SubmitJobRequest request) throws InterruptedException {
    String jobId = submitJob(request);
    JobDetails details;
    int i = 0;
    do {
      Thread.sleep(WAIT_BEFORE_PROGRESS_CHECK_IN_MILLIS);
      details = getProgressWithRetry(jobId);
      if (++i % PROGRESS_PRINT_INTERVAL == 0) {
        LOGGER.info("Progress: {}", details);
      }
    } while (!END_STATES.contains(details.getState()));
    if(!details.getState().equals(STATE_FINISHED)) {
      throw new RuntimeException("Job execution finished with state: " + details.getState());
    }

    LOGGER.info("Job finished! Details: {}", details);
  }

  private JobDetails getProgressWithRetry(String jobId) throws InterruptedException {
    int i = 0;
    while (true) {
      try {
        try {
          return getProgress(jobId);
        } catch (RestClientResponseException e) {
          if (e.getStatusCode() == HttpStatus.NOT_FOUND &&
              e.getResponseBodyAsString().contains("org.apache.flink.runtime.rest.NotFoundException")) {
            throw new RuntimeException("There is no more job of the id: " + jobId + " on the server", e);
          }
          throw e;
        }
      } catch (RestClientException e) {
        LOGGER.warn("Exception while getting the job progress! Waiting for retry", e);
        Thread.sleep(SLEEP_BETWEEN_RETRIES);
        if (++i > MAX_RETRIES) {
          throw e;
        }
      }
    }
  }

  public JobDetails getProgress(String jobId) {
    return progressRestTemplate.exchange(serverUrl+"/jobs/" + jobId, HttpMethod.GET, new HttpEntity(httpHeader), JobDetails.class).getBody();
  }

  private String submitJob(SubmitJobRequest request) throws InterruptedException {
    UUID localJobId = request.getLocalJobId();
    try {
      ResponseEntity<SubmitJobResponse> response = submitRestTemplate.exchange(
          serverUrl + "/jars/" + jarId + "/run?entry-class=" + request.getEntryClass()
          , HttpMethod.POST, new HttpEntity<>(request, httpHeader), SubmitJobResponse.class);
      SubmitJobResponse responseBody = response.getBody();
      LOGGER.info("Submitted Job: {}\nSubmission result status code: {} response body:\n{}\nExecuting...",
          request, response.getStatusCode(), responseBody);
      return Optional.ofNullable(responseBody).map(SubmitJobResponse::getJobid).orElseThrow();
    } catch(RestClientException e) {
      return attemptTaskReconnectWithRetries(localJobId, e);

    }
  }

  private String attemptTaskReconnectWithRetries(UUID localJobId, RestClientException e) throws InterruptedException {
    int i = 0;
    while (true) {
      List<String> recentJobIds = getRecentTasksJobIds();
      String externalJobId = checkRecentTasksLocalJobIds(localJobId, recentJobIds);
      if (externalJobId == null){
        if (++i > MAX_RETRIES) {
            throw e;
          }
        Thread.sleep(SLEEP_BETWEEN_RETRIES);
        LOGGER.warn("Exception while submitting task! Waiting for retry", e);
        continue;
      }
      return externalJobId;
    }
  }

  private String checkRecentTasksLocalJobIds(UUID localJobId, List<String> recentJobIds) {
    for (String jobId : recentJobIds) {
      ResponseEntity<JobConfigResponse> configResponse = submitRestTemplate.exchange(
              serverUrl + "/jobs/" + jobId + "/config",
              HttpMethod.GET, new HttpEntity<>(httpHeader),
              JobConfigResponse.class);
      //Checks if job local id is matching with response local id
      boolean isMatchingJob = Optional.ofNullable(configResponse.getBody())
              .map(JobConfigResponse::getExecutionConfig)
              .map(ExecutionConfig::getUserConfig)
              .map(UserConfig::getLocalJobId)
              .map(responseLocalJobId -> responseLocalJobId.equals(localJobId))
              .orElse(false);
      if(isMatchingJob) {
        return jobId;
      }
    }
    return null;
  }

  private List<String> getRecentTasksJobIds() {
    ResponseEntity<JobOverviewResponse> response = submitRestTemplate.exchange(
          serverUrl + "/jobs/overview", HttpMethod.GET,
            new HttpEntity<>(httpHeader),
            JobOverviewResponse.class);

    long currentTimestamp = System.currentTimeMillis();
    long recentTimeThresholdTimestamp = currentTimestamp - RECENT_TASK_THRESHOLD;
    //Gets all recent tasks job ids
    return Optional.ofNullable(response.getBody())
            .map(JobOverviewResponse::getJobs)
            .map(jobs -> filterRecentJobs(recentTimeThresholdTimestamp, jobs))
            .map(JobExecutor::collectJobIds).orElse(Collections.emptyList());
  }

  private static List<String> collectJobIds(List<Job> jobs) {
    return jobs.stream()
            .map(Job::getJid).toList();
  }

  private static List<Job> filterRecentJobs(long recentTimeThresholdTimestamp, List<Job> jobs) {
    return jobs.stream()
            .filter(job -> job.getStartTime() > recentTimeThresholdTimestamp).toList();
  }

  private RestTemplate createSubmitRestTemplate() {
    final RestTemplate restTemplate = new RestTemplate();
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECTION_TIMEOUT_FOR_SUBMIT_REQUEST);
    requestFactory.setReadTimeout(READ_TIMEOUT_FOR_SUBMIT_REQUEST);
    restTemplate.setRequestFactory(requestFactory);
    return restTemplate;
  }

  private RestTemplate createProgressRestTemplate() {
    final RestTemplate restTemplate = new RestTemplate();
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECTION_TIMEOUT_FOR_PROGRESS_REQUEST);
    requestFactory.setReadTimeout(READ_TIMEOUT_FOR_PROGRESS_REQUEST);
    restTemplate.setRequestFactory(requestFactory);
    return restTemplate;
  }

}
