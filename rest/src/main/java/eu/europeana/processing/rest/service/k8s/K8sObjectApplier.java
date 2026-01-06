package eu.europeana.processing.rest.service.k8s;

import eu.europeana.processing.rest.config.ApplicationConfiguration;
import eu.europeana.processing.rest.exception.ApplicationException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sends k8s objects to cluster
 */
@Service
public class K8sObjectApplier {

  private static final Logger LOGGER = LoggerFactory.getLogger(K8sObjectApplier.class);

  private final CoreV1Api api;
  private final AppsV1Api appsApi;
  private final BatchV1Api batchApi;
  private final ApplicationConfiguration applicationConfiguration;

  /**
   * Service constructor
   *
   * @param api {@link CoreV1Api}
   * @param appsApi {@link AppsV1Api}
   * @param batchApi {@link BatchV1Api}
   * @param applicationConfiguration {@link ApplicationConfiguration}
   */
  public K8sObjectApplier(CoreV1Api api, AppsV1Api appsApi, BatchV1Api batchApi, ApplicationConfiguration applicationConfiguration) {
    this.api = api;
    this.appsApi = appsApi;
    this.batchApi = batchApi;
    this.applicationConfiguration = applicationConfiguration;
  }

  /**
   * Deploys given secret to cluster
   *
   * @param secret {@link V1Secret}
   * @throws ApplicationException exceptin
   */
  public void deploySecret(V1Secret secret) throws ApplicationException {
    try {
      api.createNamespacedSecret(applicationConfiguration.k8sClusterNamespace(), secret).execute();
    } catch (ApiException e) {
      throw new ApplicationException(e);
    }
  }

  /**
   * Deploys given service to cluster
   *
   * @param service {@link V1Service}
   * @throws ApplicationException exception
   */
  public void deployService(V1Service service) throws ApplicationException {
    try {
      api.createNamespacedService(applicationConfiguration.k8sClusterNamespace(), service).execute();
    } catch (ApiException e) {
      throw new ApplicationException(e);
    }
  }

  /**
   * Deploys given deployment to cluster
   *
   * @param deployment {@link V1Deployment}
   * @throws ApplicationException exception
   */
  public void deployDeployment(V1Deployment deployment) throws ApplicationException {
    try {
      appsApi.createNamespacedDeployment(applicationConfiguration.k8sClusterNamespace(), deployment).execute();
    } catch (ApiException e) {
      throw new ApplicationException(e);
    }
  }

  /**
   * Deploys gicen job to cluster
   *
   * @param job {@link V1Job}
   * @throws ApplicationException exception
   */
  public void deployJob(V1Job job) throws ApplicationException {
    try {
      batchApi.createNamespacedJob(applicationConfiguration.k8sClusterNamespace(), job).execute();
    } catch (ApiException e) {
      throw new ApplicationException(e);
    }
  }

  /**
   * Removes secret with given name from k8s cluster
   *
   * @param secretName name of the secret that will be removed
   */
  public void removeSecret(String secretName) {
    try {
      api.deleteNamespacedSecret(
          secretName,
          applicationConfiguration.k8sClusterNamespace()).execute();
    } catch (ApiException e) {
      LOGGER.error("Failed to remove secret named '{}'", secretName, e);
    }
  }

  /**
   * Removes service with given name from k8s cluster
   *
   * @param serviceName name of the service that will be removed
   */
  public void removeService(String serviceName) {
    try {
      api.deleteNamespacedService(
          serviceName,
          applicationConfiguration.k8sClusterNamespace()).execute();
    } catch (ApiException e) {
      LOGGER.error("Failed to remove service named '{}'", serviceName, e);
    }
  }

  /**
   * Removes job with given name from k8s cluster
   *
   * @param jobName name of the job that will be removed
   */
  public void removeJob(String jobName) {
    try {
      batchApi.deleteNamespacedJob(
          jobName,
          applicationConfiguration.k8sClusterNamespace()).execute();
    } catch (ApiException e) {
      LOGGER.error("Failed to remove job for {}", jobName, e);
    }
  }

  public void removeDeployment(String deploymentName) {
    try {
      appsApi.deleteNamespacedDeployment(deploymentName, applicationConfiguration.k8sClusterNamespace()).execute();
    } catch (ApiException e) {
      LOGGER.error("Failed to remove deployment for {}", deploymentName, e);
    }
  }

}
