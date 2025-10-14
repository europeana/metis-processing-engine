package eu.europeana.processing.rest.tool;

import eu.europeana.processing.model.TaskInfo;
import java.util.Locale;

/**
 * Generates names for k8s objects
 */
public final class K8sObjectNameGenerator {

  private K8sObjectNameGenerator(){}

  /**
   * Generates service name for given task
   * @param taskInfo {@link TaskInfo}
   * @return service name
   */
  public static String generateServiceName(TaskInfo taskInfo) {
    return generateJobName(taskInfo) + "-service";
  }

  /**
   * Generates job name for given task
   * @param taskInfo {@link TaskInfo}
   * @return job name
   */
  public static String generateJobName(TaskInfo taskInfo) {
    return taskInfo.getTaskName()
                   .toLowerCase(Locale.getDefault())
                   .replace("_", "-") + "-job-" + taskInfo.getTaskId();

  }

  /**
   * Generates secret name for given task
   * @param taskInfo {@link TaskInfo}
   * @return secret name
   */
  public static String generateConfigSecretName(TaskInfo taskInfo) {
    return generateJobName(taskInfo) + "-config";
  }

  /**
   * Generates deployment name for given task
   * @param taskInfo {@link TaskInfo}
   * @return deployment name
   */
  public static String generateDeploymentName(TaskInfo taskInfo) {
    return generateJobName(taskInfo) + "-task-manager";
  }
}
