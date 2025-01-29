package eu.europeana.processing.validation;

import eu.europeana.processing.job.JobParamName;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Validates parameters provided for job with the internal data source (currently all jobs except OAI-PMH job).
 */
public abstract class AbstractInternalSourceJobValidator extends JobParamValidator {

  public void validate(ParameterTool parameterTool) {
    validateDBRelatedParameters(parameterTool);
    validateJobRelatedParameters(parameterTool);
    validateJobSpecificParameters(parameterTool);
  }

  private void validateJobRelatedParameters(ParameterTool parameterTool) {
    parameterTool.getRequired(JobParamName.DATASET_ID);
    parameterTool.getRequired(JobParamName.EXECUTION_ID);
  }

}
