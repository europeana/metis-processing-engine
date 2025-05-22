package eu.europeana.cloud.flink.client;

import eu.europeana.cloud.flink.client.entities.*;

import java.util.*;

import eu.europeana.cloud.flink.client.entities.JobConfigResponse.ExecutionConfig;
import eu.europeana.cloud.flink.client.entities.JobConfigResponse.ExecutionConfig.UserConfig;
import eu.europeana.cloud.flink.client.entities.JobOverviewResponse.Job;
import eu.europeana.cloud.flink.client.exceptions.SubmitJobException;
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
public interface JobExecutor {

  String STATE_FINISHED = "FINISHED";
  String STATE_CANCELED = "CANCELED";
  String STATE_FAILED = "FAILED";
  Set<String> END_STATES = Set.of(STATE_FINISHED, STATE_FAILED, STATE_CANCELED);
  long WAIT_BEFORE_PROGRESS_CHECK_IN_MILLIS = 200;
  /**
   * @param request submit job request that contain program arguments and job configuration.
   * @throws InterruptedException if thread is interrupted
   * Executes the job on flink cluster and waits for it to finish.
   * In case of error during submission will try to reconnect to task.
   * When job is being executed it prints progress.
   * In case of successful execution, job details are printed.
   * Otherwise, exception is thrown.
   */
  void execute(SubmitJobRequest request) throws InterruptedException;
}
