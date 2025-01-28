package eu.europeana.cloud.flink.client;

import eu.europeana.cloud.flink.client.entities.JobDetails;
import eu.europeana.cloud.flink.client.entities.SubmitJobRequest;
import eu.europeana.cloud.flink.client.entities.SubmitJobResponse;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
  public static final int MAX_RETRIES = 20;
  public static final long SLEEP_BETWEEN_RETRIES = 15000L;
  private static final int CONNECTION_TIMEOUT_FOR_SUBMIT_REQUEST = 60_000;
  private static final int READ_TIMEOUT_FOR_SUBMIT_REQUEST = 60_000;
  private static final int CONNECTION_TIMEOUT_FOR_PROGRESS_REQUEST = 10_000;
  private static final int READ_TIMEOUT_FOR_PORGRESS_REQUEST = 10_000;
  private static final long WAIT_BEFORE_PROGRESS_CHECK_IN_MILLIS = 200;
  private static final long PROGRESS_PRINT_INTERVAL = 5;

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
    System.out.println("");
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

  private String submitJob(SubmitJobRequest request) {
    SubmitJobResponse result = submitRestTemplate.exchange(
        serverUrl + "/jars/" + jarId + "/run?entry-class=" + request.getEntryClass()
        , HttpMethod.POST, new HttpEntity<>(request, httpHeader), SubmitJobResponse.class).getBody();
    LOGGER.info("Submitted Job: {} Submission result:\n{}\nExecuting...", request, result);
    return result.getJobid();
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
    requestFactory.setReadTimeout(READ_TIMEOUT_FOR_PORGRESS_REQUEST);
    restTemplate.setRequestFactory(requestFactory);
    return restTemplate;
  }

}
