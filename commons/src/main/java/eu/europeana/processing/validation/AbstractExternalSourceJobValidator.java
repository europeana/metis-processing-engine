package eu.europeana.processing.validation;

import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Validates parameters provided for job with the external data source (currently just OAI-PMH job).
 */
public abstract class AbstractExternalSourceJobValidator extends JobParamValidator {

  public void validate(ParameterTool parameterTool) {
    validateDBRelatedParameters(parameterTool);
    validateJobSpecificParameters(parameterTool);
  }

}
