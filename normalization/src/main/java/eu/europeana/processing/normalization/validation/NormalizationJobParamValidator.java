package eu.europeana.processing.normalization.validation;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.validation.JobParamValidator;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Validates parameters provided for {@link eu.europeana.processing.normalization.NormalizationJob}
 * during task startup.
 */

public class NormalizationJobParamValidator implements JobParamValidator {
    @Override
    public void validate(ParameterTool parameterTool) {

        parameterTool.getRequired(JobParamName.DATASET_ID);
        parameterTool.getRequired(JobParamName.EXECUTION_ID);
    }
}
