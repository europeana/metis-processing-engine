package eu.europeana.processing.validation;

import eu.europeana.processing.job.JobName;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.test.common.AbstractJobTest;
import org.junit.jupiter.api.Test;

class ExternalValidationJobTest extends AbstractJobTest {

  @Test
  void shouldProperlyRunExternalValidation() throws Exception {
    startPostgresDbServer("validation-external-input.sql");
    String[] args = prepareArgs("--" + JobParamName.VALIDATION_TYPE, JobName.VALIDATION_EXTERNAL);

    ValidationJob.main(args);

    assertThatResultRowIsSavedInDb();
    assertNoErrorsSavedInDb();
  }

  @Test
  void shouldSaveRecordErrorInDB() throws Exception {
    startPostgresDbServer("invalid-external-input.sql");
    String[] args = prepareArgs("--" + JobParamName.VALIDATION_TYPE, JobName.VALIDATION_EXTERNAL);

    ValidationJob.main(args);

    assertThatErrorIsSavedInDb();
  }

  @Override
  protected int stepNumber() {
    return 2;
  }

}

