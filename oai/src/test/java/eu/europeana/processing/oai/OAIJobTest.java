package eu.europeana.processing.oai;

import eu.europeana.processing.test.common.AbstractJobTest;
import org.junit.jupiter.api.Test;

class OAIJobTest extends AbstractJobTest {

  @Test
  void shouldProperlyHarvestRecords() throws Exception {
    startPostgresDbServer();
    String[] args = prepareArgs(
        "--oaiRepositoryUrl", "https://metis-repository-rest.test.eanadev.org/repository/oai",
        "--metadataPrefix", "edm",
        "--setSpec", "Heide1record");

    OAIJob.main(args);

    assertThatResultRowIsSavedInDb();
    assertNoErrorsSavedInDb();
  }

  @Override
  protected int stepNumber() {
    return 1;
  }
}