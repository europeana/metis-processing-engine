package eu.europeana.processing.rest.service.k8s;

import eu.europeana.processing.rest.config.AppConfig;
import eu.europeana.processing.rest.exception.ApplicationException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import org.springframework.stereotype.Service;

/**
 * Sends k8s objects to cluster
 */
@Service
public class K8sObjectApplier {

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
  public K8sObjectApplier(CoreV1Api api, AppsV1Api appsApi, BatchV1Api batchApi, AppConfig appConfig) {
    this.api = api;
    this.appsApi = appsApi;
    this.batchApi = batchApi;
    this.appConfig = appConfig;
  }

  /**
   * Deploys given secret to cluster
   *
   * @param secret {@link V1Secret}
   * @throws ApplicationException exceptin
   */
  public void deploySecret(V1Secret secret) throws ApplicationException {
    try {
      api.createNamespacedSecret(appConfig.k8sClusterNamespace(), secret).execute();
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
      api.createNamespacedService(appConfig.k8sClusterNamespace(), service).execute();
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
      appsApi.createNamespacedDeployment(appConfig.k8sClusterNamespace(), deployment).execute();
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
      batchApi.createNamespacedJob(appConfig.k8sClusterNamespace(), job).execute();
    } catch (ApiException e) {
      throw new ApplicationException(e);
    }
  }
}
