package eu.europeana.processing.enrichment.validation;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.validation.JobParamValidator;
import org.apache.flink.api.java.utils.ParameterTool;

public class EnrichmentJobParamValidator implements JobParamValidator {

    @Override
    public void validate(ParameterTool parameterTool) {

        parameterTool.getRequired(JobParamName.DATASET_ID);
        parameterTool.getRequired(JobParamName.EXECUTION_ID);
        parameterTool.getRequired(JobParamName.DEREFERENCE_SERVICE_URL);
        parameterTool.getRequired(JobParamName.ENRICHMENT_ENTITY_MANAGEMENT_URL);
        parameterTool.getRequired(JobParamName.ENRICHMENT_ENTITY_API_KEY);
        parameterTool.getRequired(JobParamName.ENRICHMENT_ENTITY_API_URL);
    }
}
