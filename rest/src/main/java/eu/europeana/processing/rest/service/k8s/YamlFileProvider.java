package eu.europeana.processing.rest.service.k8s;

import eu.europeana.processing.rest.exception.ApplicationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.springframework.data.repository.init.ResourceReader;

/**
 * Provides yaml files from the configuration location
 */
public class YamlFileProvider {


  private static final String FLINK_CONFIG_LOCATION = "/k8s/config.yaml";
  private static final String SERVICE_CONFIG_LOCATION = "/k8s/service.yaml";
  private static final String JOB_CONFIG_LOCATION = "/k8s/job.yaml";
  private static final String TASK_MANGER_CONFIG_LOCATION = "/k8s/task-manager.yaml";

  /**
   * Provides flink config
   *
   * @return flink config
   * @throws ApplicationException exception
   */
  public String provideFLinkClusterConfiguration() throws ApplicationException {

    try {
      InputStream is = ResourceReader.class.getResourceAsStream(FLINK_CONFIG_LOCATION);
      return IOUtils.toString(is, StandardCharsets.UTF_8);
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
    try(InputStream is = ResourceReader.class.getResourceAsStream(SERVICE_CONFIG_LOCATION)) {
      return IOUtils.toString(is, StandardCharsets.UTF_8);
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
      InputStream is = ResourceReader.class.getResourceAsStream(JOB_CONFIG_LOCATION);
      return IOUtils.toString(is, StandardCharsets.UTF_8);
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
      InputStream is = ResourceReader.class.getResourceAsStream(TASK_MANGER_CONFIG_LOCATION);
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
}
