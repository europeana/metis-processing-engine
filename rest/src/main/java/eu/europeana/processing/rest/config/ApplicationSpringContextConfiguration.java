package eu.europeana.processing.rest.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring configuration for application
 */
@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties({
    ApplicationConfiguration.class})
@EnableScheduling
public class ApplicationSpringContextConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationSpringContextConfiguration.class);

  /**
   * Client for kubernetes
   *
   * @param applicationConfiguration {@link ApplicationConfiguration}
   * @return {@link CoreV1Api}
   */
  @Bean
  public CoreV1Api api(ApplicationConfiguration applicationConfiguration) {

    LOGGER.info("Initializing CoreV1Api");

    try {
      ApiClient client = Config.fromToken(
          applicationConfiguration.k8sClusterLocation(),
          Files.readString(Path.of(applicationConfiguration.k8sClusterAccessKeyFileLocation())).trim()
      );
      return new CoreV1Api(client);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Client for kubernetes
   *
   * @param applicationConfiguration {@link ApplicationConfiguration}
   * @return {@link AppsV1Api}
   */
  @Bean
  public AppsV1Api appsApi(ApplicationConfiguration applicationConfiguration) {
    LOGGER.info("Initializing AppsV1Api");

    try {
      ApiClient client = Config.fromToken(
          applicationConfiguration.k8sClusterLocation(),
          Files.readString(Path.of(applicationConfiguration.k8sClusterAccessKeyFileLocation())).trim()
      );
      return new AppsV1Api(client);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Client for kubernetes
   *
   * @param applicationConfiguration {@link ApplicationConfiguration}
   * @return {@link BatchV1Api}
   */
  @Bean
  public BatchV1Api batchApi(ApplicationConfiguration applicationConfiguration) {
    LOGGER.info("Initializing BatchV1Api");

    try {
      ApiClient client = Config.fromToken(
          applicationConfiguration.k8sClusterLocation(),
          Files.readString(Path.of(applicationConfiguration.k8sClusterAccessKeyFileLocation())).trim()
      );
      return new BatchV1Api(client);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
