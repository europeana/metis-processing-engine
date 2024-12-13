package eu.europeana.processing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "test")
public class TestConfigurationProperties {

  long pauseBetweenTestsMs;
  String datasetId;
  DbCleaningMode dbCleaning;
}
