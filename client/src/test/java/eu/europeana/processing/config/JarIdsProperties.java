package eu.europeana.processing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "flink.jar.id")
public class JarIdsProperties {
  private String oai;
  private String validation;
  private String transformation;
  private String normalization;
  private String enrichment;
  private String media;
  private String indexing;
}
