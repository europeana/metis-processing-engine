package eu.europeana.processing.rest.service.k8s;

import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.rest.config.AppConfig;
import eu.europeana.processing.rest.tool.K8sObjectNameGenerator;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reads k8s objects deployed on cluster
 */
@Service
public class K8sObjectRetriever {

  private static final Logger LOGGER = LoggerFactory.getLogger(K8sObjectRetriever.class);

  private final BatchV1Api batchApi;
  private final AppConfig appConfig;

  /**
   * Constructor
   *
   * @param batchApi {@link BatchV1Api}
   * @param appConfig {@link AppConfig}
   */
  public K8sObjectRetriever(BatchV1Api batchApi, AppConfig appConfig) {
    this.batchApi = batchApi;
    this.appConfig = appConfig;
  }

  /**
   * Reads k8s job deployed on cluster for given task
   * @param taskInfo {@link TaskInfo}
   * @return {@link V1Job}
   * @throws ApiException exception
   */
  public V1Job retrieveJob(TaskInfo taskInfo) throws ApiException {
    LOGGER.debug("Getting job object for given task");
    return batchApi.readNamespacedJob(K8sObjectNameGenerator.generateJobName(taskInfo), appConfig.k8sClusterNamespace())
                   .execute();
  }
}
