package eu.europeana.processing.normalization;

import eu.europeana.processing.test.common.AbstractJobTest;
import org.junit.jupiter.api.Test;

class NormalizationJobTest extends AbstractJobTest {

  @Test
  void shouldProperlyRunNormalization() throws Exception {
    startPostgresDbServer("normalization-input.sql");
    String[] args = prepareArgs();

    NormalizationJob.main(args);

    assertThatResultRowIsSavedInDb();
    assertNoErrorsSavedInDb();
  }

  @Test
  void shouldSaveRecordErrorInDB() throws Exception {
    startPostgresDbServer("invalid-record-input.sql");
    String[] args = prepareArgs();

    NormalizationJob.main(args);

    assertThatErrorIsSavedInDb();
  }

  @Override
  protected int stepNumber() {
    return 5;
  }

}