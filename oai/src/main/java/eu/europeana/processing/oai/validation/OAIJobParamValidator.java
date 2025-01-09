package eu.europeana.processing.oai.validation;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.validation.AbstractExternalSourceJobValidator;
import org.apache.flink.api.java.utils.ParameterTool;

public class OAIJobParamValidator extends AbstractExternalSourceJobValidator {

    @Override
    public void validateJobSpecificParameters(ParameterTool parameterTool) {
        parameterTool.getRequired(JobParamName.DATASET_ID);
        parameterTool.getRequired(JobParamName.SET_SPEC);
        parameterTool.getRequired(JobParamName.METADATA_PREFIX);
        parameterTool.getRequired(JobParamName.OAI_REPOSITORY_URL);
    }
}
