package eu.europeana.processing.rest.dto;

import java.util.Map;

/**
 * Contains all needed information related with submission request
 *
 * @param jobName name of hte jab
 * @param parameters job parameters
 */
public record JobSubmissionDto(String jobName, Map<String, String> parameters) {

}
