package eu.europeana.processing.http;

import static eu.europeana.processing.job.JobParamName.*;

import eu.europeana.processing.test.common.AbstractJobTest;
import org.junit.jupiter.api.Test;

class HttpHarvestingJobTest extends AbstractJobTest {

  @Test
  void shouldProperlyHarvestRecords() throws Exception {
    startPostgresDbServer();
    String[] args = prepareArgs("--" + HTTP_ARCHIVE_URL,
        "https://metis-repository-rest.test.eanadev.org/repository/zip/Heide1record.zip");

    HttpHarvestingJob.main(args);

    assertThatResultRowIsSavedInDb();
    assertNoErrorsSavedInDb();
  }

  @Override
  protected int stepNumber() {
    return 1;
  }
}