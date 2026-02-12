package eu.europeana.processing.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application configuration
 *
 * @param k8sClusterLocation location onf k8s cluster that will be used for flink jobs
 * @param k8sClusterAccessKeyFileLocation access key to the k8s cluster
 * @param k8sClusterNamespace namespace on k8s where Flink jobs will be deployed
 * @param oaiImage docker image name for oai job
 * @param httpImage docker image name for http job
 * @param validationImage docker image name for validation job
 * @param transformationImage docker image name for transformation job
 * @param normalizationImage docker image name for normalization job
 * @param enrichmentImage docker image name for enrichment job
 * @param mediaImage docker image name for media job
 * @param indexingImage docker image name for indexing job
 * @param jobsConfigurationLocation directory where configuration files for job are located
 */
@ConfigurationProperties(prefix = "app")
public record ApplicationConfiguration(
    String k8sClusterLocation,
    String k8sClusterAccessKeyFileLocation,
    String k8sClusterNamespace,
    String oaiImage,
    String httpImage,
    String validationImage,
    String transformationImage,
    String normalizationImage,
    String enrichmentImage,
    String mediaImage,
    String indexingImage,
    String jobsConfigurationLocation,
    boolean jobCleaningServiceEnabled
) {
}
