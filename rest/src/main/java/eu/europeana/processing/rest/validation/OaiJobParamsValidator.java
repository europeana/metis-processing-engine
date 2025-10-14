package eu.europeana.processing.rest.validation;

import eu.europeana.processing.job.JobParamName;
import java.util.Map;

/**
 * Validates parameters provided in {@link eu.europeana.processing.rest.dto.JobSubmissionDto} for oai job
 */

public class OaiJobParamsValidator implements JobParamsValidator {

  @Override
  public boolean validate(Map<String, String> parameters) {
    return parameters.containsKey(JobParamName.DATASET_ID) &&
        parameters.containsKey(JobParamName.SET_SPEC) &&
        parameters.containsKey(JobParamName.METADATA_PREFIX) &&
        parameters.containsKey(JobParamName.OAI_REPOSITORY_URL);
  }
}
