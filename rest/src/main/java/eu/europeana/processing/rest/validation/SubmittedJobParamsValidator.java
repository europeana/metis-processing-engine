package eu.europeana.processing.rest.validation;

import eu.europeana.processing.model.JobName;
import eu.europeana.processing.rest.dto.JobSubmissionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Validates {@link JobSubmissionDto} object
 */
@Service
public class SubmittedJobParamsValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubmittedJobParamsValidator.class);

  /**
   * Does actual validation
   * @param jobSubmissionDto {@link JobSubmissionDto} with parameters
   * @return validation result
   */
  public boolean validate(JobSubmissionDto jobSubmissionDto) {
    LOGGER.info("Validating job submission params for: {}", jobSubmissionDto);

    JobParamsValidator validator = null;
    if (jobSubmissionDto.jobName().equalsIgnoreCase(JobName.OAI_HARVEST)) {
      validator = new OaiJobParamsValidator();
    } else if (jobSubmissionDto.jobName().equalsIgnoreCase(JobName.VALIDATION_EXTERNAL)) {
      validator = new ValidationJobParamsValidator();
    } else if (jobSubmissionDto.jobName().equalsIgnoreCase(JobName.TRANSFORMATION)) {
      validator = new TransformationJobParamsValidator();
    } else if (jobSubmissionDto.jobName().equalsIgnoreCase(JobName.VALIDATION_INTERNAL)) {
      validator = new ValidationJobParamsValidator();
    } else if (jobSubmissionDto.jobName().equalsIgnoreCase(JobName.NORMALIZATION)) {
      validator = new NormalizationJobParamsValidator();
    } else if (jobSubmissionDto.jobName().equalsIgnoreCase(JobName.ENRICHMENT)) {
      validator = new EnrichmentJobParamsValidator();
    } else if (jobSubmissionDto.jobName().equalsIgnoreCase(JobName.MEDIA)) {
      validator = new MediaJobParamsValidator();
    } else if (jobSubmissionDto.jobName().equalsIgnoreCase(JobName.INDEXING)) {
      validator = new IndexingJobParamsValidator();
    } else {
      return false;
    }
    return validator.validate(jobSubmissionDto.parameters());
  }
}
