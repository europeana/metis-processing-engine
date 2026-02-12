package eu.europeana.processing.rest.service.k8s;

import eu.europeana.processing.rest.config.ApplicationConfiguration;
import eu.europeana.processing.rest.exception.ApplicationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

/**
 * Provides yaml files from the configuration location
 */
@Service
public class YamlFileProvider {

  private static final String FLINK_CONFIG_FILE_NAME = "/config.yaml";
  private static final String SERVICE_CONFIG_FILE_NAME = "/service.yaml";
  private static final String JOB_CONFIG_FILE_NAME = "/job.yaml";
  private static final String TASK_MANGER_CONFIG_FLE_NAME = "/task-manager.yaml";
  private final ApplicationConfiguration applicationConfiguration;

  public YamlFileProvider(ApplicationConfiguration applicationConfiguration) {
    this.applicationConfiguration = applicationConfiguration;
  }
  /**
   * Provides flink config
   *
   * @return flink config
   * @throws ApplicationException exception
   */
  public String provideFLinkClusterConfiguration() throws ApplicationException {

    try {
      Path path = Path.of(applicationConfiguration.jobsConfigurationLocation() + FLINK_CONFIG_FILE_NAME);
      return Files.readString(path);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  /**
   * Provides Service config
   * @return service config
   * @throws ApplicationException exception
   */
  public String provideFlinkJobService() throws ApplicationException {
    try {
      Path path = Path.of(applicationConfiguration.jobsConfigurationLocation() + SERVICE_CONFIG_FILE_NAME);
      return Files.readString(path);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  /**
   * Provides job config
   * @return job config
   * @throws ApplicationException exception
   */
  public String provideFlinkJobConfiguration() throws ApplicationException {
    try {
      Path path = Path.of(applicationConfiguration.jobsConfigurationLocation() + JOB_CONFIG_FILE_NAME);
      return Files.readString(path);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }

  /**
   * Provides task manager config
   * @return task manager config
   * @throws ApplicationException exception
   */
  public String provideFlinkTaskManagerConfiguration() throws ApplicationException {
    try {
      Path path = Path.of(applicationConfiguration.jobsConfigurationLocation() + TASK_MANGER_CONFIG_FLE_NAME);
      return Files.readString(path);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
}
