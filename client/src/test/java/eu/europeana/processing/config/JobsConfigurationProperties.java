package eu.europeana.processing.config;

import eu.europeana.processing.config.job.EnrichmentConfigurationProperties;
import eu.europeana.processing.config.job.IndexingConfigurationProperties;
import eu.europeana.processing.config.job.MediaConfigurationProperties;
import eu.europeana.processing.config.job.NormalizationConfigurationProperties;
import eu.europeana.processing.config.job.OaiHarvestConfigurationProperties;
import eu.europeana.processing.config.job.TransformationConfigurationProperties;
import eu.europeana.processing.config.job.ValidationConfigurationProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Setter
@Getter
@ConfigurationProperties(prefix = "job")
public class JobsConfigurationProperties {

  @NestedConfigurationProperty
  private OaiHarvestConfigurationProperties oaiHarvest;
  @NestedConfigurationProperty
  private ValidationConfigurationProperties validation;
  @NestedConfigurationProperty
  private TransformationConfigurationProperties transformation;
  @NestedConfigurationProperty
  private NormalizationConfigurationProperties normalization;
  @NestedConfigurationProperty
  private EnrichmentConfigurationProperties enrichment;
  @NestedConfigurationProperty
  private MediaConfigurationProperties media;
  @NestedConfigurationProperty
  private IndexingConfigurationProperties indexing;
}
