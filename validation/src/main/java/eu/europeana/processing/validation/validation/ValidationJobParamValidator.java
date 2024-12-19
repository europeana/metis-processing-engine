package eu.europeana.processing.validation.validation;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.validation.AbstractInternalSourceJobValidator;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Validates parameters provided for {@link eu.europeana.processing.validation.ValidationJob}
 * during task startup.
 */
public class ValidationJobParamValidator extends AbstractInternalSourceJobValidator {

    @Override
    public void validateJobSpecificParameters(ParameterTool parameterTool) {
        parameterTool.getRequired(JobParamName.VALIDATION_TYPE);
    }
}
