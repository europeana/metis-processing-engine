package eu.europeana.cloud.flink.client.session;

import eu.europeana.cloud.flink.client.JobExecutor;
import eu.europeana.cloud.flink.client.entities.JobConfigResponse;
import eu.europeana.cloud.flink.client.entities.JobConfigResponse.ExecutionConfig;
import eu.europeana.cloud.flink.client.entities.JobConfigResponse.ExecutionConfig.UserConfig;
import eu.europeana.cloud.flink.client.entities.JobDetails;
import eu.europeana.cloud.flink.client.entities.JobOverviewResponse;
import eu.europeana.cloud.flink.client.entities.JobOverviewResponse.Job;
import eu.europeana.cloud.flink.client.entities.SubmitJobRequest;
import eu.europeana.cloud.flink.client.entities.SubmitJobResponse;
import eu.europeana.cloud.flink.client.exceptions.SubmitJobException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
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

/**
 * Class responsible for executing flink jobs via flink rest api
 */
public class SessionClusterJobExecutor implements JobExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SessionClusterJobExecutor.class);
  public static final int MAX_RETRIES_FOR_PROGRESS_REQUEST = 20;
  public static final long SLEEP_BETWEEN_RETRIES_FOR_PROGRESS_REQUEST = 15000L;
  public static final int MAX_RETRIES_FOR_SUBMIT_REQUEST = 30;
  public static final long SLEEP_BETWEEN_RETRIES_FOR_SUBMIT_REQUEST = 200L;
  private static final int CONNECTION_TIMEOUT_FOR_SUBMIT_REQUEST = 60_000;
  private static final int READ_TIMEOUT_FOR_SUBMIT_REQUEST = 60_000;
  private static final int CONNECTION_TIMEOUT_FOR_PROGRESS_REQUEST = 10_000;
  private static final int READ_TIMEOUT_FOR_PROGRESS_REQUEST = 10_000;

  private static final long PROGRESS_PRINT_INTERVAL = 5;
  private static final int RECENT_TASK_THRESHOLD = 10 * 60 * 1000;

  private final String jarId;
  private final RestTemplate submitRestTemplate;
  private final RestTemplate progressRestTemplate;
  private final String serverUrl;
  private final HttpHeaders httpHeader;

  public SessionClusterJobExecutor(Properties serverConfiguration) {
    this(serverConfiguration.getProperty("job.manager.url"),
        serverConfiguration.getProperty("job.manager.user"),
        serverConfiguration.getProperty("job.manager.password"),
        serverConfiguration.getProperty("jar.id"));
  }

  public SessionClusterJobExecutor(AbstractEnvironment serverConfiguration) {
    this(serverConfiguration.getProperty("job.manager.url"),
        serverConfiguration.getProperty("job.manager.user"),
        serverConfiguration.getProperty("job.manager.password"),
        serverConfiguration.getProperty("jar.id"));
  }

  /**
   * @param serverUrl Flink server url
   * @param user Flink user
   * @param password Flink password
   * @param jarId Flink jar id
   */
  public SessionClusterJobExecutor(String serverUrl, String user, String password, String jarId) {
    this.serverUrl = serverUrl;
    httpHeader = new HttpHeaders();
    httpHeader.setBasicAuth(user, password);
    //We create different templates for submit job request which could take dozens of seconds, and we need bigger timeout,
    // than for progress request, which is fast, and we want to have fast reaction to measure test time accurately.
    submitRestTemplate = createSubmitRestTemplate();
    progressRestTemplate = createProgressRestTemplate();
    this.jarId = jarId;
  }

  @Override
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

  /**
   * @param jobId id of the job
   * @return job details containing job state, name and its id
   * @throws InterruptedException if thread is interrupted
   */
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
        Thread.sleep(SLEEP_BETWEEN_RETRIES_FOR_PROGRESS_REQUEST);
        if (++i > MAX_RETRIES_FOR_PROGRESS_REQUEST) {
          throw e;
        }
      }
    }
  }

  /**
   * @param jobId id of the job
   * @return job details containing job state, name and its id
   */
  public JobDetails getProgress(String jobId) {
    return progressRestTemplate.exchange(serverUrl+"/jobs/" + jobId,
            HttpMethod.GET, new HttpEntity(httpHeader), JobDetails.class).getBody();
  }


  /**
   * @param request submit job request that contain program arguments and job configuration.
   * @return job id of the submitted job
   * @throws InterruptedException if thread is interrupted
   * Submits the job to flink cluster. In case of error during submission will try to reconnect to task.
   * Algorithm of reconnection goes as follows:
   * When job is submitted local job id is generated in form of UUID,
   * that is later on included in the request config part as local job id.
   * When exception is thrown during process of task submission,
   * we don't know exact state of task since we don't parse exception message.
   * There are two cases:
   * 1. Task could be submitted, and we didn't receive response,
   * 2. task could be submitted, and we received response with error.
   * We need to assume that it is first case and try to reconnect to task.
   * That is because if we retry task submission we can potentially clone task on flink cluster.
   * Reconnection steps:
   * 1. Filter recent jobs from job list (/jobs/overview) and get their external job ids.
   * 2. Get each of recent jobs config (/jobs/<jobid>/config).
   * 3. Check for match of local job id that was included in the request at beginning of the submission process
   * and one included in task request config part local id.
   * 4a. In case of match we assume that task was submitted,
   * and we can reconnect to it by using external job id that we got in step 1.
   * 4b. In case of no match we assume that task was not submitted, so we retry and repeat those steps.
   * 5. In case of no match after retries we throw exception. After that user need to manually resubmit task.
   */
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
      LOGGER.warn("Exception occurred during task submission process.", e);
      return attemptTaskReconnectWithRetries(localJobId, e);

    }
  }

  private String attemptTaskReconnectWithRetries(UUID localJobId, RestClientException e) throws InterruptedException {
    int i = 0;
    while (true) {
      Optional<String> externalJobId = findRecentlySubmittedJobByLocalId(localJobId);
      if (externalJobId.isEmpty()){
        if (++i > MAX_RETRIES_FOR_SUBMIT_REQUEST) {
            throw new SubmitJobException("Job submission state is ambiguous and wasn't able to reconnect to task.", e);
          }
        Thread.sleep(SLEEP_BETWEEN_RETRIES_FOR_SUBMIT_REQUEST);
        LOGGER.warn("Exception occurred when reconnecting to potentially submitted task! Retrying");
        continue;
      }
      return externalJobId.get();
    }
  }

  private Optional<String> findRecentlySubmittedJobByLocalId(UUID localJobId) {
    try{
      for (String jobId : getRecentTasksJobIds()) {
        ResponseEntity<JobConfigResponse> configResponse = submitRestTemplate.exchange(
                serverUrl + "/jobs/" + jobId + "/config",
                HttpMethod.GET, new HttpEntity<>(httpHeader),
                JobConfigResponse.class);
        if(isJobMatching(localJobId, configResponse)) {
          return Optional.of(jobId);
        }
      }
    } catch(RestClientException e){
      LOGGER.warn("Exception occurred when trying to find recently submitted job", e);
    }
    return Optional.empty();
  }

  /**
   * @param localJobId local job id
   * @param configResponse config response
   * @return if job config local id matches response local id
   */
  private static boolean isJobMatching(UUID localJobId, ResponseEntity<JobConfigResponse> configResponse) {
    return Optional.ofNullable(configResponse.getBody())
            .map(JobConfigResponse::getExecutionConfig)
            .map(ExecutionConfig::getUserConfig)
            .map(UserConfig::getLocalJobId)
            .map(responseLocalJobId -> responseLocalJobId.equals(localJobId))
            .orElse(false);
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
            .stream()
            .flatMap(List::stream)
            .filter(job -> job.getStartTime() > recentTimeThresholdTimestamp)
            .map(Job::getJid)
            .toList();
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
