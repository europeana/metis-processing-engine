package eu.europeana.processing.validation;

import eu.europeana.processing.job.JobParamName;
import org.apache.flink.api.java.utils.ParameterTool;

public class ValidationJobParamValidator implements JobParamValidator {
    @Override
    public void validate(ParameterTool parameterTool) {

        parameterTool.getRequired(JobParamName.DATASET_ID);
        parameterTool.getRequired(JobParamName.EXECUTION_ID);
        parameterTool.getRequired(JobParamName.VALIDATION_TYPE);
    }
}
