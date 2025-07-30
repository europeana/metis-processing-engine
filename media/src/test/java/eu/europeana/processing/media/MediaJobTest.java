package eu.europeana.processing.media;

import eu.europeana.processing.test.common.AbstractJobTest;
import org.junit.jupiter.api.Test;

class MediaJobTest extends AbstractJobTest {
  @Test
  void shouldProperlyRunMedia() throws Exception {
    startPostgresDbServer("media-input.sql");
    String[] args = prepareArgs();

    MediaJob.main(args);

    assertThatResultRowIsSavedInDb();
    assertNoErrorsSavedInDb();
  }

  @Test
  void shouldSaveRecordErrorInDB() throws Exception {
    startPostgresDbServer("invalid-record-input.sql");
    String[] args = prepareArgs();

    MediaJob.main(args);

    assertThatErrorIsSavedInDb();
  }

  @Override
  protected int stepNumber() {
    return 7;
  }
}