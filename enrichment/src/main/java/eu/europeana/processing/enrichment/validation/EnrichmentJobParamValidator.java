package eu.europeana.processing.enrichment.validation;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.validation.AbstractInternalSourceJobValidator;
import org.apache.flink.util.ParameterTool;

/**
 * Validates parameters provided for {@link eu.europeana.processing.enrichment.EnrichmentJob} during task startup.
 */
public class EnrichmentJobParamValidator extends AbstractInternalSourceJobValidator {

  @Override
  public void validateJobSpecificParameters(ParameterTool parameterTool) {
    parameterTool.getRequired(JobParamName.DEREFERENCE_SERVICE_URL);
    parameterTool.getRequired(JobParamName.ENRICHMENT_ENTITY_MANAGEMENT_URL);
    parameterTool.getRequired(JobParamName.ENRICHMENT_ENTITY_API_TOKEN_ENDPOINT);
    parameterTool.getRequired(JobParamName.ENRICHMENT_ENTITY_API_GRANT_PARAMS);
    parameterTool.getRequired(JobParamName.ENRICHMENT_ENTITY_API_URL);
  }
}
