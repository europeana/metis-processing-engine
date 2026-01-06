package eu.europeana.cloud.flink.client.application;

import eu.europeana.cloud.flink.client.JobExecutor;
import eu.europeana.cloud.flink.client.entities.SubmitJobRequest;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationClusterJobExecutor implements JobExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationClusterJobExecutor.class);
  private static final String CLUSTER_ID_PLACEHOLDER = "cluster-id-template-to-replace";

  private final CoreV1Api api;
  private final BatchV1Api batchApi;
  private final ApiClient client;
  private final String namespace;
  private final AppsV1Api appsApi;
  private final Path configTemplateDir;

  public ApplicationClusterJobExecutor(String namespace, Path configTemplateDir) throws IOException, ApiException {
    this.configTemplateDir = configTemplateDir;
    this.namespace = namespace;
    client = Config.defaultClient();
    api = new CoreV1Api(client);
    appsApi = new AppsV1Api(client);
    batchApi = new BatchV1Api(client);
    List<String> podNames = getNamesOfPodsOnCluster();
    LOGGER.info("Created ApplicationClusterJobExecutor - pods present in the namespace: {} : {}", namespace, podNames);
  }

  @Override
  public void execute(SubmitJobRequest request) throws InterruptedException {
    try {
      String jobId = UUID.randomUUID().toString().replace("-", "");
      try {
        deployJobOnOpenshift(request, jobId);
        waitForJobCompletion(jobId);
      } finally {
        clearAfterJob(jobId);
      }
    } catch (IOException | ApiException e) {
      throw new ApplicationClusterException("Could not execute Job!", e);
    }

  }

  private void clearAfterJob(String jobId) throws ApiException {
    LOGGER.info("TaskManager deletion: {}", appsApi.deleteNamespacedDeployment(createTaskManagerName(jobId), namespace).execute().getStatus());
    LOGGER.info("Service deletion: {}", api.deleteNamespacedService(createServiceName(jobId), namespace).execute().getStatus());
    LOGGER.info("Job deletetion: {}", batchApi.deleteNamespacedJob(createJobName(jobId), namespace).execute().getStatus());
    LOGGER.info("Job config deletetion: {}",api.deleteNamespacedSecret(createConfigSecretName(jobId), namespace).execute().getStatus());
  }

  private void waitForJobCompletion(String jobId) throws ApiException, InterruptedException {
    while (true) {
      Thread.sleep(WAIT_BEFORE_PROGRESS_CHECK_IN_MILLIS);
      V1Job job = batchApi.readNamespacedJob(createJobName(jobId), namespace).execute();
      V1JobStatus status = job.getStatus();
      if (status != null) {
        LOGGER.info("Job status, active: {}", status.getActive());
        if (status.getSucceeded() != null && status.getSucceeded() > 0) {
          LOGGER.info("Job completed successfully!");
          return;
        }

        if (status.getFailed() != null && status.getFailed() > 0) {
          throw new ApplicationClusterException("Job failed!");
        }
      }

    }
  }

  private void deployJobOnOpenshift(SubmitJobRequest request, String jobId) throws IOException, ApiException {
    deployConfiguration(jobId);
    createJobInstance(jobId, request);
    deployService(jobId);
    deployTaskManagers(jobId);
    printPods();
  }

  private void deployService(String jobId) throws IOException, ApiException {
    V1Service service = Yaml.loadAs(configTemplateDir.resolve("service.yaml").toFile(), V1Service.class);
    service.getSpec().setSelector(Map.of("job-name", createJobName(jobId)));
    service.getMetadata().setName(createServiceName(jobId));
    api.createNamespacedService(namespace, service).execute();
  }

  private void deployConfiguration(String jobId) throws IOException, ApiException {
    String configContent = Files.readString(configTemplateDir.resolve("config.yaml"));
    configContent = configContent.replace(CLUSTER_ID_PLACEHOLDER, createJobName(jobId) + "-cluster");
    V1Secret secret = new V1Secret()
        .apiVersion("v1")
        .kind("Secret")
        .metadata(new V1ObjectMeta()
            .name(createConfigSecretName(jobId))
            .namespace(namespace))
        .type("Opaque")
        .stringData(Map.of("config.yaml", configContent));
    api.createNamespacedSecret(namespace, secret).execute();
  }

  private void deployTaskManagers(String jobId) throws IOException, ApiException {
    String yamlContent = Files.readString(configTemplateDir.resolve("task-manager.yaml"));
    yamlContent = yamlContent.replace("flink-config-secret-template-to-replace", createConfigSecretName(jobId));
    V1Deployment deployment = Yaml.loadAs(yamlContent, V1Deployment.class);
    deployment.getMetadata().setName(createTaskManagerName(jobId));
    appsApi.createNamespacedDeployment(namespace, deployment).execute();
  }

  private void createJobInstance(String jobId, SubmitJobRequest request) throws IOException, ApiException {
    String yamlContent = Files.readString(configTemplateDir.resolve("job.yaml"));
    yamlContent = yamlContent.replace("flink-config-secret-template-to-replace", createConfigSecretName(jobId));
    V1Job jobDefinition = Yaml.loadAs(yamlContent, V1Job.class);

    jobDefinition.getMetadata().setName(createJobName(jobId));

    jobDefinition.getSpec().getTemplate().getSpec().getContainers().get(0).setArgs(createArgs(jobId, request));
    batchApi.createNamespacedJob(namespace, jobDefinition).execute();
  }

  private List<String> createArgs(String jobId, SubmitJobRequest request) {
    List<String> args = new ArrayList<>(List.of("standalone-job", "--job-id", jobId));
    args.addAll(request.getProgramArgsList());
    return args;
  }

  private void printPods() throws ApiException {
    LOGGER.info("Pods on the cluster in namespace: {} : {}", namespace, getNamesOfPodsOnCluster());
  }

  private List<String> getNamesOfPodsOnCluster() throws ApiException {
    List<V1Pod> pods = api.listNamespacedPod(namespace).execute().getItems();
    return pods.stream().map(pod -> pod.getMetadata().getName()).toList();
  }

  private static String createTaskManagerName(String jobId) {
    return createJobName(jobId) + "-task-manager";
  }

  private static String createServiceName(String jobId) {
    return createJobName(jobId) + "-service";
  }

  private static String createJobName(String jobId) {
    return "job-" + jobId;
  }

  private static String createConfigSecretName(String jobId) {
    return createJobName(jobId) + "-config";
  }




}
