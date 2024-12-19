package eu.europeana.processing.transformation.validation;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.validation.AbstractInternalSourceJobValidator;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Validates parameters provided for {@link eu.europeana.processing.transformation.TransformationJob} during task startup.
 */
public class TransformationJobParamValidator extends AbstractInternalSourceJobValidator {

    @Override
    public void validateJobSpecificParameters(ParameterTool parameterTool) {
        parameterTool.getRequired(JobParamName.METIS_DATASET_NAME);
        parameterTool.getRequired(JobParamName.METIS_DATASET_COUNTRY);
        parameterTool.getRequired(JobParamName.METIS_DATASET_LANGUAGE);
        parameterTool.getRequired(JobParamName.METIS_XSLT_URL);
    }
}
