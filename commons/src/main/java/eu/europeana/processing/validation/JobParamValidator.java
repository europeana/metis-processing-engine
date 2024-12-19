package eu.europeana.processing.validation;

import eu.europeana.processing.job.JobParamName;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Validates parameters provided for job during task startup.
 */
public abstract class JobParamValidator {

  protected void validateDBRelatedParameters(ParameterTool parameterTool) {
    parameterTool.getRequired(JobParamName.DATASOURCE_URL);
    parameterTool.getRequired(JobParamName.DATASOURCE_USERNAME);
    parameterTool.getRequired(JobParamName.DATASOURCE_PASSWORD);
  }

  /**
   * Does the actual validation of the parameters.
   *
   * @param parameterTool container for parameters
   */
  public abstract void validate(ParameterTool parameterTool);

  /**
   * Validates job specific parameters. This method should be overridden in subclasses if needed.
   * Default implementation does nothing as if there are no job specific parameters.
   *
   * @param parameterTool container for parameters
   */
  public void validateJobSpecificParameters(ParameterTool parameterTool) {
    // do nothing by default
  }
}
