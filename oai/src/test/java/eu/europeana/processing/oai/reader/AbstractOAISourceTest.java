package eu.europeana.processing.oai.reader;

import static eu.europeana.processing.job.JobParamName.DATASET_ID;
import static eu.europeana.processing.job.JobParamName.TASK_ID;
import static java.lang.String.valueOf;

import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.oai.repository.OAIHeadersRepository;
import java.time.Instant;
import java.util.Map;
import org.apache.flink.api.java.utils.ParameterTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

public abstract class AbstractOAISourceTest {
  protected static final String DATASET = "dataset";
  protected static final String TASK = String.valueOf(11);

  protected static final Instant DATESTAMP_A =Instant.parse("2024-12-10T00:00:00Z");
  protected static final Instant DATESTAMP_B =Instant.parse("2024-12-01T00:00:00Z");
  protected static final OaiRecordHeader HEADER_1=new OaiRecordHeader(
      "ecloud_e2e_tests_without_4_records_ecloud_e2e_tests_NLS____NLS2__RS_664______2WGTWS8_sr",
      false, DATESTAMP_A);
  protected static final OaiRecordHeader HEADER_2=new OaiRecordHeader(
      "ecloud_e2e_tests_without_4_records_ecloud_e2e_tests_NLS____NLS2__RS_643______06VMZI9_sr",
      false, DATESTAMP_A);
  protected static final OaiRecordHeader HEADER_3=new OaiRecordHeader(
      "ecloud_e2e_tests_without_4_records_ecloud_e2e_tests_NLS____NLS2__RS_486______3OL0PS4_sr",
      false, DATESTAMP_B);
  protected static final OaiRecordHeader HEADER_4=new OaiRecordHeader(
      "ecloud_e2e_tests_without_4_records_ecloud_e2e_tests_NLS____NLS2__RS_388______0Y4FH46_sr",
      false, DATESTAMP_B);

  protected MockedConstruction<DbConnectionProvider> dbProviderConstruction;
  protected MockedConstruction<OAIHeadersRepository> repositoryConstruction;
  protected ParameterTool parameterTool;

  @BeforeEach
  final void setupCommon() {
    parameterTool = ParameterTool.fromMap(Map.of(DATASET_ID, DATASET, TASK_ID, valueOf(TASK)));
    dbProviderConstruction= Mockito.mockConstruction(DbConnectionProvider.class);
  }

  @AfterEach
  final void cleanupConstructions() {
    repositoryConstruction.close();
    dbProviderConstruction.close();
  }
}
