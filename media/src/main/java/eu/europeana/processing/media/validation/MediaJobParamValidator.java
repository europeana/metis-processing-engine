package eu.europeana.processing.media.validation;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.validation.JobParamValidator;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Validates parameters provided for {@link eu.europeana.processing.media.MediaJob}
 * during task startup.
 */

public class MediaJobParamValidator implements JobParamValidator {

    @Override
    public void validate(ParameterTool parameterTool) {
        parameterTool.getRequired(JobParamName.DATASET_ID);
        parameterTool.getRequired(JobParamName.EXECUTION_ID);
    }
}
