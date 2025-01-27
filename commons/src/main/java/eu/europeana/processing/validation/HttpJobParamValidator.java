package eu.europeana.processing.validation;

import eu.europeana.processing.job.JobParamName;
import org.apache.flink.api.java.utils.ParameterTool;

public class HttpJobParamValidator extends AbstractExternalSourceJobValidator {

    @Override
    public void validateJobSpecificParameters(ParameterTool parameterTool) {
        parameterTool.getRequired(JobParamName.DATASET_ID);
        parameterTool.getRequired(JobParamName.HTTP_ARCHIVE_URL);
    }
}
