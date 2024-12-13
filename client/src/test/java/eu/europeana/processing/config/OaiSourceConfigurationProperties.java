package eu.europeana.processing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "source")
public class OaiSourceConfigurationProperties {

  String url;
  String setSpec;
  String metadataPrefix;
  int recordCount;
  int validRecordCount = -1;

  public int getValidRecordCount() {
    if (validRecordCount == -1) {
      return recordCount;
    } else {
      return validRecordCount;
    }
  }

}

