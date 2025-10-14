package eu.europeana.processing.rest.service.k8s;

import static eu.europeana.processing.job.JobParamName.DATASOURCE_PASSWORD;
import static eu.europeana.processing.job.JobParamName.DATASOURCE_URL;
import static eu.europeana.processing.job.JobParamName.DATASOURCE_USERNAME;
import static eu.europeana.processing.job.JobParamName.TASK_ID;

import com.zaxxer.hikari.HikariConfig;
import eu.europeana.processing.model.JobName;
import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.rest.config.AppConfig;
import eu.europeana.processing.rest.tool.K8sObjectNameGenerator;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.util.Yaml;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Converts strings to different kubernetes objects
 */
@Service
public class K8sObjectGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(K8sObjectGenerator.class);

  protected HikariConfig dbConfig;
  private final AppConfig appConfig;

  /**
   * Constructor
   *
   * @param appConfig {@link AppConfig}
   * @param dbConfig {@link HikariConfig}
   */
  public K8sObjectGenerator(AppConfig appConfig, HikariConfig dbConfig) {
    this.appConfig = appConfig;
    this.dbConfig = dbConfig;
  }

  /**
   * Generates k8s secret specific for given task
   * @param config content of the configuration file
   * @param taskInfo task definition
   * @return {@link V1Secret}
   */
  public V1Secret generateConfigurationAsSecretWithPayloadForTask(String config, TaskInfo taskInfo) {

    return new V1Secret()
        .apiVersion("v1")
        .kind("Secret")
        .metadata(new V1ObjectMeta()
            .name(K8sObjectNameGenerator.generateConfigSecretName(taskInfo))
            .namespace(appConfig.k8sClusterNamespace()))
        .type("Opaque")
        .stringData(Map.of("config.yaml", config));
  }

  /**
   * Generates k8s job specific for given task
   * @param config content of the configuration file
   * @param taskInfo task definition
   * @return {@link V1Job}
   */
  public V1Job generateConfigurationForJobAndTask(String config, TaskInfo taskInfo) {
    V1Job jobDefinition = Yaml.loadAs(config, V1Job.class);
    setupConfigVolumeName(taskInfo, jobDefinition);
    setupJobName(jobDefinition, taskInfo);
    setupImageName(jobDefinition, taskInfo);
    setupJobParameters(jobDefinition, taskInfo);
    LOGGER.info("Generated Job Definition: {}", jobDefinition);
    return jobDefinition;
  }

  private void setupJobParameters(V1Job jobDefinition, TaskInfo taskInfo) {
    jobDefinition.getSpec().getTemplate().getSpec().getContainers().get(0).setArgs(createArgs(taskInfo));
  }

  private List<String> createArgs(TaskInfo taskInfo) {
    List<String> args = new ArrayList<>(List.of("standalone-job"));

    //parameters from request
    Map<String, String> requestParams = taskInfo.getParameters();

    Map<String, Object> internalParams = new HashMap<>(
        Map.of(DATASOURCE_URL, dbConfig.getJdbcUrl(), DATASOURCE_USERNAME, dbConfig.getUsername(), DATASOURCE_PASSWORD,
            dbConfig.getPassword(), TASK_ID, taskInfo.getTaskId()));

    requestParams.forEach((key, value) -> {
      args.add("--" + key);
      args.add(String.valueOf(value));
    });

    internalParams.forEach((key, value) -> {
      args.add("--" + key);
      args.add(String.valueOf(value));
    });
    return args;
  }

  private void setupImageName(V1Job jobDefinition, TaskInfo taskInfo) {
    String imageName = "default";

    if (taskInfo.getTaskName().equalsIgnoreCase(JobName.OAI_HARVEST)) {
      imageName = appConfig.oaiImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.HTTP_HARVEST)) {
      imageName = appConfig.httpImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.VALIDATION_EXTERNAL)) {
      imageName = appConfig.validationImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.TRANSFORMATION)) {
      imageName = appConfig.transformationImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.NORMALIZATION)) {
      imageName = appConfig.normalizationImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.ENRICHMENT)) {
      imageName = appConfig.enrichmentImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.MEDIA)) {
      imageName = appConfig.mediaImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.INDEXING)) {
      imageName = appConfig.indexingImage();
    }

    jobDefinition.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(imageName);
  }

  private void setupImageName(V1Deployment deploymentDefinition, TaskInfo taskInfo) {
    String imageName = "default";

    if (taskInfo.getTaskName().equalsIgnoreCase(JobName.OAI_HARVEST)) {
      imageName = appConfig.oaiImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.HTTP_HARVEST)) {
      imageName = appConfig.httpImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.VALIDATION_EXTERNAL)) {
      imageName = appConfig.validationImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.TRANSFORMATION)) {
      imageName = appConfig.transformationImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.NORMALIZATION)) {
      imageName = appConfig.normalizationImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.ENRICHMENT)) {
      imageName = appConfig.enrichmentImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.MEDIA)) {
      imageName = appConfig.mediaImage();
    } else if (taskInfo.getTaskName().equalsIgnoreCase(JobName.INDEXING)) {
      imageName = appConfig.indexingImage();
    }

    deploymentDefinition.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(imageName);
  }

  private void setupJobName(V1Job jobDefinition, TaskInfo taskInfo) {
    jobDefinition.getMetadata().setName(K8sObjectNameGenerator.generateJobName(taskInfo));
  }

  private void setupConfigVolumeName(TaskInfo taskInfo, V1Job jobDefinition) {
    List<V1Volume> volumes = jobDefinition.getSpec().getTemplate().getSpec().getVolumes();
    volumes.forEach(volume -> {
      if (volume.getName().equals("flink-config-volume")) {
        volume.getSecret().setSecretName(K8sObjectNameGenerator.generateConfigSecretName(taskInfo));
      }
    });
  }

  public V1Deployment generateConfigurationForDeployment(String config, TaskInfo taskInfo) {
    config = config.replace("flink-config-secret-template-to-replace", K8sObjectNameGenerator.generateConfigSecretName(taskInfo));
    V1Deployment deployment = Yaml.loadAs(config, V1Deployment.class);
    setupImageName(deployment, taskInfo);
    deployment.getMetadata().setName(K8sObjectNameGenerator.generateDeploymentName(taskInfo));
    return deployment;
  }

  public V1Service generateConfigurationForService(String config, TaskInfo taskInfo) {
    V1Service service = Yaml.loadAs(config, V1Service.class);
    service.getSpec().setSelector(Map.of("job-name", K8sObjectNameGenerator.generateJobName(taskInfo)));
    service.getMetadata().setName(K8sObjectNameGenerator.generateServiceName(taskInfo));
    return service;
  }
}
