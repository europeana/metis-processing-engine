package eu.europeana.processing.validation;

import eu.europeana.processing.job.JobName;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.test.common.AbstractJobTest;
import org.junit.jupiter.api.Test;

class InternalValidationJobTest extends AbstractJobTest {

  @Test
  void shouldProperlyRunInternalValidation() throws Exception {
    startPostgresDbServer("validation-internal-input.sql");
    String[] args = prepareArgs("--" + JobParamName.VALIDATION_TYPE, JobName.VALIDATION_INTERNAL);

    ValidationJob.main(args);

    assertThatResultRowIsSavedInDb();
    assertNoErrorsSavedInDb();
  }

  @Test
  void shouldSaveRecordErrorInDB() throws Exception {
    startPostgresDbServer("invalid-internal-input.sql");
    String[] args = prepareArgs("--" + JobParamName.VALIDATION_TYPE, JobName.VALIDATION_INTERNAL);

    ValidationJob.main(args);

    assertThatErrorIsSavedInDb();
  }
  @Override
  protected int stepNumber() {
    return 4;
  }

}

