package eu.europeana.processing.http.validation;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.validation.AbstractExternalSourceJobValidator;
import org.apache.flink.util.ParameterTool;

/**
 * Validator for the HttpHarvestingJob
 */
public class HttpJobParamValidator extends AbstractExternalSourceJobValidator {

    @Override
    public void validateJobSpecificParameters(ParameterTool parameterTool) {
        parameterTool.getRequired(JobParamName.DATASET_ID);
        parameterTool.getRequired(JobParamName.HTTP_ARCHIVE_URL);
    }
}
