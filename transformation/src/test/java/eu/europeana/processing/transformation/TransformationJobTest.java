package eu.europeana.processing.transformation;

import eu.europeana.processing.job.JobParamName;
import org.junit.jupiter.api.Test;
import eu.europeana.processing.test.common.AbstractJobTest;

class TransformationJobTest extends AbstractJobTest {

  @Test
  void shouldProperlyRunTransformation() throws Exception {
    startPostgresDbServer("transformation-input.sql");
    String[] args = prepareArgs("--" + JobParamName.METIS_DATASET_NAME, "idA_metisDatasetNameA",
        "--" + JobParamName.METIS_DATASET_COUNTRY, "Greece",
        "--" + JobParamName.METIS_DATASET_LANGUAGE, "el",
        "--" + JobParamName.METIS_XSLT_URL, "https://metis-core-rest.test.eanadev.org/datasets/xslt/6204e5e2514e773e6745f7e9");

    TransformationJob.main(args);

    assertThatResultRowIsSavedInDb();
    assertNoErrorsSavedInDb();
  }


  @Test
  void shouldSaveRecordErrorInDB() throws Exception {
    startPostgresDbServer("invalid-record-input.sql");
    String[] args = prepareArgs("--" + JobParamName.METIS_DATASET_NAME, "idA_metisDatasetNameA",
        "--" + JobParamName.METIS_DATASET_COUNTRY, "Greece",
        "--" + JobParamName.METIS_DATASET_LANGUAGE, "el",
        "--" + JobParamName.METIS_XSLT_URL, "https://metis-core-rest.test.eanadev.org/datasets/xslt/6204e5e2514e773e6745f7e9");

    TransformationJob.main(args);

    assertThatErrorIsSavedInDb();
  }

  @Override
  protected int stepNumber() {
    return 3;
  }

}