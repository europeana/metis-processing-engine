package eu.europeana.processing.rest.service;

import eu.europeana.processing.rest.config.AppConfig;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api.APIlistNamespacedJobRequest;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Cleans already finished jobs from k8s cluster
 */
@Service
public class JobCleaningService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JobCleaningService.class);

  private static final long DAYS_TO_KEEP_JOBS = 0;

  private final CoreV1Api api;
  private final AppsV1Api appsApi;
  private final BatchV1Api batchApi;
  private final AppConfig appConfig;

  /**
   * Service constructor
   *
   * @param api {@link CoreV1Api}
   * @param appsApi {@link AppsV1Api}
   * @param batchApi {@link BatchV1Api}
   * @param appConfig {@link AppConfig}
   */
  public JobCleaningService(CoreV1Api api, AppsV1Api appsApi, BatchV1Api batchApi, AppConfig appConfig) {
    this.api = api;
    this.appsApi = appsApi;
    this.batchApi = batchApi;
    this.appConfig = appConfig;
  }

  /**
   * Scans all the jobs and removed that can be removed
   */
  @Scheduled(fixedRate = 60000)
  public void cleanJobs() {

    try {
      List<V1Job> v1Jobs = readAllJobs();
      LOGGER.info("Cleaning jobs at cluster");
      for (V1Job job : v1Jobs) {
        LOGGER.info("Cleaning job at '{}'", job.getMetadata().getName());
        if (eligibleToBeRemoved(job)) {
          LOGGER.info("Removing job at cluster {}", job.getMetadata().getLabels().get("job-name"));
          removeSecret(job);
          removeDeployment(job);
          removeService(job);
          removeJob(job);
        } else {
          LOGGER.info("Job will not be removed {}", job.getMetadata().getLabels().get("job-name"));
        }
      }
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
    LOGGER.info("Cleaning finished");
  }

  private void removeJob(V1Job job) {
    try {
      batchApi.deleteNamespacedJob(
          job.getMetadata().getLabels().get("job-name"),
          appConfig.k8sClusterNamespace()).execute();
    } catch (ApiException e) {
      LOGGER.error("Failed to remove job for {}", job.getMetadata().getLabels().get("job-name"), e);
    }
  }

  private void removeService(V1Job job) {
    try {
      api.deleteNamespacedService(
          job.getMetadata().getLabels().get("job-name") + "service",
          appConfig.k8sClusterNamespace()).execute();
    } catch (ApiException e) {
      LOGGER.error("Failed to remove service for {}", job.getMetadata().getName(), e);
    }
  }

  private void removeDeployment(V1Job job) {
    try {
      appsApi.deleteNamespacedDeployment(
          job.getMetadata().getLabels().get("job-name") + "-task-manager",
          appConfig.k8sClusterNamespace()).execute();
    } catch (ApiException e) {
      LOGGER.error("Failed to remove job deployment for '{}'", job.getMetadata().getName(), e);
    }
  }

  private void removeSecret(V1Job job) {
    try {
      api.deleteNamespacedSecret(
          job.getMetadata().getLabels().get("job-name") + "-service",
          appConfig.k8sClusterNamespace()).execute();
    } catch (ApiException e) {
      LOGGER.error("Failed to remove secret for '{}'", job.getMetadata().getName(), e);
    }
  }

  private boolean eligibleToBeRemoved(V1Job job) {
    return job.getStatus() !=null &&
        job.getStatus().getFailed() != null &&
        job.getStatus().getFailed() > 0 &&
        job.getStatus().getStartTime() != null &&
        job.getStatus().getStartTime().plusDays(DAYS_TO_KEEP_JOBS).isBefore(OffsetDateTime.now());
  }

  private List<V1Job> readAllJobs() throws ApiException {
    APIlistNamespacedJobRequest apIlistNamespacedJobRequest = batchApi.listNamespacedJob(appConfig.k8sClusterNamespace());
    return apIlistNamespacedJobRequest.execute().getItems();
  }
}
