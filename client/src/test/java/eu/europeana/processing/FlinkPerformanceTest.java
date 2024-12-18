package eu.europeana.processing;

import static eu.europeana.processing.job.JobParamName.CHUNK_SIZE;
import static eu.europeana.processing.job.JobParamName.DATASET_ID;
import static eu.europeana.processing.job.JobParamName.DATASOURCE_PASSWORD;
import static eu.europeana.processing.job.JobParamName.DATASOURCE_URL;
import static eu.europeana.processing.job.JobParamName.DATASOURCE_USERNAME;
import static eu.europeana.processing.job.JobParamName.DEREFERENCE_SERVICE_URL;
import static eu.europeana.processing.job.JobParamName.ENRICHMENT_ENTITY_API_KEY;
import static eu.europeana.processing.job.JobParamName.ENRICHMENT_ENTITY_API_URL;
import static eu.europeana.processing.job.JobParamName.ENRICHMENT_ENTITY_MANAGEMENT_URL;
import static eu.europeana.processing.job.JobParamName.EXECUTION_ID;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOAPPLICATIONNAME;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOAUTHDB;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGODBNAME;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOINSTANCES;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOPASSWORD;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOPOOLSIZE;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOPORTNUMBER;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOREADPREFERENCE;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOREDIRECTDBNAME;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOUSERNAME;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOUSESSL;
import static eu.europeana.processing.job.JobParamName.INDEXING_PERFORMREDIRECTS;
import static eu.europeana.processing.job.JobParamName.INDEXING_PRESERVETIMESTAMPS;
import static eu.europeana.processing.job.JobParamName.INDEXING_SOLRINSTANCES;
import static eu.europeana.processing.job.JobParamName.INDEXING_ZOOKEEPERCHROOT;
import static eu.europeana.processing.job.JobParamName.INDEXING_ZOOKEEPERDEFAULTCOLLECTION;
import static eu.europeana.processing.job.JobParamName.INDEXING_ZOOKEEPERINSTANCES;
import static eu.europeana.processing.job.JobParamName.INDEXING_ZOOKEEPERPORTNUMBER;
import static eu.europeana.processing.job.JobParamName.MAX_RECORD_PENDING;
import static eu.europeana.processing.job.JobParamName.METADATA_PREFIX;
import static eu.europeana.processing.job.JobParamName.METIS_DATASET_COUNTRY;
import static eu.europeana.processing.job.JobParamName.METIS_DATASET_LANGUAGE;
import static eu.europeana.processing.job.JobParamName.METIS_DATASET_NAME;
import static eu.europeana.processing.job.JobParamName.METIS_XSLT_URL;
import static eu.europeana.processing.job.JobParamName.OAI_REPOSITORY_URL;
import static eu.europeana.processing.job.JobParamName.OPERATOR_PARALLELISM;
import static eu.europeana.processing.job.JobParamName.READER_PARALLELISM;
import static eu.europeana.processing.job.JobParamName.SET_SPEC;
import static eu.europeana.processing.job.JobParamName.SINK_PARALLELISM;
import static eu.europeana.processing.job.JobParamName.TASK_ID;
import static eu.europeana.processing.job.JobParamName.VALIDATION_TYPE;

import eu.europeana.cloud.flink.client.JobExecutor;
import eu.europeana.cloud.flink.client.entities.SubmitJobRequest;
import eu.europeana.processing.config.FlinkConfigurationProperties;
import eu.europeana.processing.config.JarIdsProperties;
import eu.europeana.processing.config.JobsConfigurationProperties;
import eu.europeana.processing.job.JobParamValue;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.time.StopWatch;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


public class FlinkPerformanceTest extends AbstractPerformanceTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private FlinkConfigurationProperties flinkConfigurationProperties;

  @Autowired
  protected JobsConfigurationProperties jobsConfigurationProperties;

  @Autowired
  protected JarIdsProperties jarIdsProperties;

  @Test
  void step1_shouldExecuteOAIHarvestComplietellyWithoutErrors() throws Exception {

    executeStep(1, jarIdsProperties.getOai(), "eu.europeana.processing.oai.OAIJob",
        Map.of(OAI_REPOSITORY_URL, sourceProperties.getUrl(), SET_SPEC, sourceProperties.getSetSpec(), METADATA_PREFIX,
            sourceProperties.getMetadataPrefix()));
  }

  @Test
  void step2_shouldExecuteExternalValidationWithoutErrors() throws Exception {
    executeStep(2, jarIdsProperties.getValidation(), "eu.europeana.processing.validation.ValidationJob",
        Map.of(VALIDATION_TYPE, JobParamValue.VALIDATION_EXTERNAL));
  }


  @Test
  void step3_shouldExecuteXsltTransformationWithoutErrors() throws Exception {
    executeStep(3, jarIdsProperties.getTransformation(), "eu.europeana.processing.transformation.TransformationJob",
        Map.of(METIS_DATASET_NAME, "idA_metisDatasetNameA", METIS_DATASET_COUNTRY, "Greece", METIS_DATASET_LANGUAGE, "el",
            METIS_XSLT_URL, "https://metis-core-rest.test.eanadev.org/datasets/xslt/6204e5e2514e773e6745f7e9"));
  }

  @Test
  void step4_shouldExecuteIternalValidationWithoutErrors() throws Exception {
    executeStep(4, jarIdsProperties.getValidation(), "eu.europeana.processing.validation.ValidationJob",
        Map.of(VALIDATION_TYPE, JobParamValue.VALIDATION_INTERNAL));
  }

  @Test
  void step5_shouldExecuteNormalizationWithoutErrors() throws Exception {
    executeStep(5, jarIdsProperties.getNormalization(), "eu.europeana.processing.normalization.NormalizationJob",
        Collections.emptyMap());
  }

  @Test
  void step6_shouldExecuteEnrichmentWithoutErrors() throws Exception {
    executeStep(6, jarIdsProperties.getEnrichment(), "eu.europeana.processing.enrichment.EnrichmentJob",
        Map.of(DEREFERENCE_SERVICE_URL, jobsConfigurationProperties.getEnrichment().getDereferenceUrl(),
            ENRICHMENT_ENTITY_MANAGEMENT_URL, jobsConfigurationProperties.getEnrichment().getEntityManagementUrl(),
            ENRICHMENT_ENTITY_API_URL, jobsConfigurationProperties.getEnrichment().getEntityApiUrl(), ENRICHMENT_ENTITY_API_KEY,
            jobsConfigurationProperties.getEnrichment().getEntityApiKey()));

  }

  @Test
  void step7_shouldExecuteMediaWithoutErrors() throws Exception {
    executeStep(7, jarIdsProperties.getMedia(), "eu.europeana.processing.media.MediaJob", Collections.emptyMap());
  }

  @Test
  void step8_shouldExecuteIndexingWithoutErrors() throws Exception {
    Map<String, String> specialParameters = new HashMap<>();
    specialParameters.put(INDEXING_PRESERVETIMESTAMPS, jobsConfigurationProperties.getIndexing().getPreserveTimestamps());
    specialParameters.put(INDEXING_PERFORMREDIRECTS, jobsConfigurationProperties.getIndexing().getPerformRedirects());
    specialParameters.put(INDEXING_MONGOINSTANCES, jobsConfigurationProperties.getIndexing().getMongoInstances());
    specialParameters.put(INDEXING_MONGOPORTNUMBER, jobsConfigurationProperties.getIndexing().getMongoPortNumber());
    specialParameters.put(INDEXING_MONGODBNAME, jobsConfigurationProperties.getIndexing().getMongoDbName());
    specialParameters.put(INDEXING_MONGOREDIRECTDBNAME, jobsConfigurationProperties.getIndexing().getMongoRedirectsDbName());
    specialParameters.put(INDEXING_MONGOUSERNAME, jobsConfigurationProperties.getIndexing().getMongoUsername());
    specialParameters.put(INDEXING_MONGOPASSWORD, jobsConfigurationProperties.getIndexing().getMongoPassword());
    specialParameters.put(INDEXING_MONGOAUTHDB, jobsConfigurationProperties.getIndexing().getMongoAuthDB());
    specialParameters.put(INDEXING_MONGOUSESSL, jobsConfigurationProperties.getIndexing().getMongoUseSSL());
    specialParameters.put(INDEXING_MONGOREADPREFERENCE, jobsConfigurationProperties.getIndexing().getMongoReadPreference());
    specialParameters.put(INDEXING_MONGOPOOLSIZE, jobsConfigurationProperties.getIndexing().getMongoPoolSize());
    specialParameters.put(INDEXING_SOLRINSTANCES, jobsConfigurationProperties.getIndexing().getMongoApplicationName());
    specialParameters.put(INDEXING_ZOOKEEPERINSTANCES, jobsConfigurationProperties.getIndexing().getZookeeperInstances());
    specialParameters.put(INDEXING_ZOOKEEPERPORTNUMBER, jobsConfigurationProperties.getIndexing().getZookeeperPortNumber());
    specialParameters.put(INDEXING_ZOOKEEPERCHROOT, jobsConfigurationProperties.getIndexing().getZookeeperChroot());
    specialParameters.put(INDEXING_ZOOKEEPERDEFAULTCOLLECTION,
        jobsConfigurationProperties.getIndexing().getZookeeperDefaultCollection());
    specialParameters.put(INDEXING_MONGOAPPLICATIONNAME,
        jobsConfigurationProperties.getIndexing().getZookeeperDefaultCollection());

    executeStep(8, jarIdsProperties.getIndexing(),
        "eu.europeana.cloud.job.indexing.IndexingJobWithPostgresMultiThreadedOperation", specialParameters);
  }

  void executeStep(int stepNumber, String jarId, String jobClass, Map<String, String> specialParameters)
      throws Exception {
    JobExecutor executor = new JobExecutor(flinkConfigurationProperties.getJobManagerUrl(),
        flinkConfigurationProperties.getJobManagerUser(),
        flinkConfigurationProperties.getJobManagerPassword(), jarId);
    beforeEach(stepNumber);
    String datasetId = testProperties.getDatasetId();
    String taskId = String.valueOf(stepNumber);

    LOGGER.info("Submitting job request datasetId: {}, taskId: {}", datasetId, taskId);
    Map<String, Object> jobParams = new HashMap<>(
        Map.of(DATASOURCE_URL, dbConfig.getJdbcUrl(), DATASOURCE_USERNAME, dbConfig.getUsername(), DATASOURCE_PASSWORD,
            dbConfig.getPassword(), DATASET_ID, datasetId, CHUNK_SIZE, flinkConfigurationProperties.getChunkSize(),
            MAX_RECORD_PENDING, flinkConfigurationProperties.getMaxRecordPending(), TASK_ID, taskId));
    if (stepNumber > 1) {
      jobParams.put(EXECUTION_ID, stepNumber - 1);
    }
    jobParams.putAll(specialParameters);
    jobParams.put(READER_PARALLELISM, flinkConfigurationProperties.getReaderParallelism());
    jobParams.put(OPERATOR_PARALLELISM, flinkConfigurationProperties.getOperatorParallelism());
    jobParams.put(SINK_PARALLELISM, flinkConfigurationProperties.getSinkParallelism());
    SubmitJobRequest request = SubmitJobRequest.builder().entryClass(jobClass)
                                               .parallelism(String.valueOf(flinkConfigurationProperties.getMaxParallelism()))
                                               .programArgs(jobParams).build();
    startWatch = StopWatch.createStarted();
    executor.execute(request);
    validateResult(stepNumber);
  }

}
