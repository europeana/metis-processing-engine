package eu.europeana.processing.rest.service.k8s;

import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.rest.exception.ApplicationException;
import eu.europeana.processing.rest.tool.K8sObjectNameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service responsible for submitting Metis jogs to k8s cluster
 */
@Service
public class FlinkJobSubmitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(FlinkJobSubmitter.class);

  private static final String JOB_MANAGER_ADDRESS_PLACEHOLDER = "<flink-job-manager>";
  private static final String CLUSTER_ID_PLACEHOLDER = "cluster-id-template-to-replace";

  private final K8sObjectApplier k8sObjectApplier;
  private final K8sObjectGenerator k8sObjectGenerator;

  /**
   * Service constructor
   *
   * @param k8sObjectApplier {@link K8sObjectApplier}
   * @param k8sObjectGenerator {@link K8sObjectGenerator}
   */
  public FlinkJobSubmitter(K8sObjectApplier k8sObjectApplier, K8sObjectGenerator k8sObjectGenerator) {
    this.k8sObjectApplier = k8sObjectApplier;
    this.k8sObjectGenerator = k8sObjectGenerator;
  }

  /**
   * Submits given task to cluster
   *
   * @param taskInfo {@link TaskInfo}
   * @throws ApplicationException exception
   */
  @Async
  public void submit(TaskInfo taskInfo) throws ApplicationException {
    LOGGER.info("Submitting Flink Job");
    submitConfiguration(taskInfo);
    submitJobManagerJob(taskInfo);
    submitJobManagerService(taskInfo);
    submitTaskManager(taskInfo);
    LOGGER.info("Flink Job submitted {}", taskInfo);
  }

  private void submitConfiguration(TaskInfo taskInfo) throws ApplicationException {

    LOGGER.debug("Submitting Flink Job configuration for {}", taskInfo);
    YamlFileProvider yamlFileProvider = new YamlFileProvider();
    String jobConfiguration = yamlFileProvider.provideFLinkClusterConfiguration();
    //
    jobConfiguration = jobConfiguration.replace(
        JOB_MANAGER_ADDRESS_PLACEHOLDER,
        K8sObjectNameGenerator.generateServiceName(taskInfo));

    jobConfiguration = jobConfiguration.replace(
        CLUSTER_ID_PLACEHOLDER,
        K8sObjectNameGenerator.generateJobName(taskInfo));
    //
    k8sObjectApplier.deploySecret(
        k8sObjectGenerator.generateConfigurationAsSecretWithPayloadForTask(jobConfiguration, taskInfo)
    );
  }

  private void submitJobManagerJob(TaskInfo taskInfo) throws ApplicationException {
    LOGGER.debug("Submitting Job manager");
    YamlFileProvider yamlFileProvider = new YamlFileProvider();
    String jobConfiguration = yamlFileProvider.provideFlinkJobConfiguration();
    k8sObjectApplier.deployJob(
        k8sObjectGenerator.generateConfigurationForJobAndTask(jobConfiguration, taskInfo)
    );
  }

  private void submitJobManagerService(TaskInfo taskInfo) throws ApplicationException {
    LOGGER.debug("Submitting Job manager service");
    YamlFileProvider yamlFileProvider = new YamlFileProvider();
    String jobConfiguration = yamlFileProvider.provideFlinkJobService();
    k8sObjectApplier.deployService(
        k8sObjectGenerator.generateConfigurationForService(jobConfiguration, taskInfo)
    );
  }

  private void submitTaskManager(TaskInfo taskInfo) throws ApplicationException {
    LOGGER.debug("Submitting Task manager deployment");
    YamlFileProvider yamlFileProvider = new YamlFileProvider();
    String jobConfiguration = yamlFileProvider.provideFlinkTaskManagerConfiguration();
    k8sObjectApplier.deployDeployment(
        k8sObjectGenerator.generateConfigurationForDeployment(jobConfiguration, taskInfo)
    );
  }
}
