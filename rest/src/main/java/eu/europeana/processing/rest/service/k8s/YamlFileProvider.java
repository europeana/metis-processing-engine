package eu.europeana.processing.rest.service.k8s;

import eu.europeana.processing.rest.exception.ApplicationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;

/**
 * Provides yaml files from the configuration location
 */
public class YamlFileProvider {


  private static final String FLINK_CONFIG_LOCATION = "config/config.yaml";
  private static final String SERVICE_CONFIG_LOCATION = "config/service.yaml";
  private static final String JOB_CONFIG_LOCATION = "config/job.yaml";
  private static final String TASK_MANGER_CONFIG_LOCATION = "config/task-manager.yaml";

  /**
   * Provides flink config
   *
   * @return flink config
   * @throws ApplicationException exception
   */
  public String provideFLinkClusterConfiguration() throws ApplicationException {

    try {
      return Files.readString(Path.of(FLINK_CONFIG_LOCATION));
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
      return Files.readString(Path.of(SERVICE_CONFIG_LOCATION));
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
      return Files.readString(Path.of(JOB_CONFIG_LOCATION));
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
      return Files.readString(Path.of(TASK_MANGER_CONFIG_LOCATION));
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
}
