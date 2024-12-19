package eu.europeana.processing.config;

import eu.europeana.processing.DbCleaner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    OaiSourceConfigurationProperties.class,
    FlinkConfigurationProperties.class,
    TestConfigurationProperties.class,
    JobsConfigurationProperties.class,
    JarIdsProperties.class})
public class TestsConfig {

  @Bean
  public DbCleaner dbCleaner() {
    return new DbCleaner();
  }
}
