package eu.europeana.processing.rest.validation;

import eu.europeana.processing.job.JobParamName;
import java.util.Map;

/**
 * Validates parameters provided in {@link eu.europeana.processing.rest.dto.JobSubmissionDto}
 * for media job
 */

public class MediaJobParamsValidator implements JobParamsValidator {

  @Override
  public boolean validate(Map<String, String> parameters) {
    return parameters.containsKey(JobParamName.DATASET_ID) &&
        parameters.containsKey(JobParamName.VALIDATION_TYPE);

  }
}
