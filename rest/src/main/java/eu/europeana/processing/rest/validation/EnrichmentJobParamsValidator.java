package eu.europeana.processing.rest.validation;

import eu.europeana.processing.job.JobParamName;
import java.util.Map;

/**
 * Validates parameters provided in {@link eu.europeana.processing.rest.dto.JobSubmissionDto}
 * for validation job
 */

public class EnrichmentJobParamsValidator implements JobParamsValidator {

  @Override
  public boolean validate(Map<String, String> parameters) {
    return parameters.containsKey(JobParamName.DATASET_ID) &&
        parameters.containsKey(JobParamName.VALIDATION_TYPE) &&
        parameters.containsKey(JobParamName.DEREFERENCE_SERVICE_URL) &&
        parameters.containsKey(JobParamName.ENRICHMENT_ENTITY_MANAGEMENT_URL) &&
        parameters.containsKey(JobParamName.ENRICHMENT_ENTITY_API_TOKEN_ENDPOINT) &&
        parameters.containsKey(JobParamName.ENRICHMENT_ENTITY_API_GRANT_PARAMS) &&
        parameters.containsKey(JobParamName.ENRICHMENT_ENTITY_API_URL);
  }
}
