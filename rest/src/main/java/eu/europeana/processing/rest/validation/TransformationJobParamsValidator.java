package eu.europeana.processing.rest.validation;

import eu.europeana.processing.job.JobParamName;
import java.util.Map;

/**
 * Validates parameters provided in {@link eu.europeana.processing.rest.dto.JobSubmissionDto}
 * for transformation job
 */
public class TransformationJobParamsValidator implements JobParamsValidator {

  @Override
  public boolean validate(Map<String, String> parameters) {
    return parameters.containsKey(JobParamName.METIS_DATASET_NAME) &&
        parameters.containsKey(JobParamName.METIS_DATASET_COUNTRY) &&
        parameters.containsKey(JobParamName.METIS_DATASET_LANGUAGE) &&
        parameters.containsKey(JobParamName.METIS_XSLT_URL);
  }
}
