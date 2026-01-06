package eu.europeana.processing.model;

import java.util.Map;

/**
 * Contains all needed information related with submission request
 *
 * @param jobName name of hte jab
 * @param parameters job parameters
 */
public record JobSubmissionDto(String jobName, Map<String, String> parameters) {

}
