package eu.europeana.processing.rest.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
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

    ApiClient client = Config.fromToken(
        applicationConfiguration.k8sClusterLocation(),
        applicationConfiguration.k8sClusterAccessKey()
    );

    return new CoreV1Api(client);
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
    ApiClient client = Config.fromToken(
        applicationConfiguration.k8sClusterLocation(),
        applicationConfiguration.k8sClusterAccessKey()
    );
    return new AppsV1Api(client);
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
    ApiClient client = Config.fromToken(
        applicationConfiguration.k8sClusterLocation(),
        applicationConfiguration.k8sClusterAccessKey()
    );
    return new BatchV1Api(client);
  }
}
