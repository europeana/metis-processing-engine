package eu.europeana.processing;

import static eu.europeana.processing.job.JobParamName.DATASET_ID;
import static eu.europeana.processing.job.JobParamName.ENRICHMENT_ENTITY_API_GRANT_PARAMS;
import static eu.europeana.processing.job.JobParamName.ENRICHMENT_ENTITY_API_TOKEN_ENDPOINT;
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
import static eu.europeana.processing.job.JobParamName.METADATA_PREFIX;
import static eu.europeana.processing.job.JobParamName.METIS_DATASET_COUNTRY;
import static eu.europeana.processing.job.JobParamName.METIS_DATASET_LANGUAGE;
import static eu.europeana.processing.job.JobParamName.METIS_DATASET_NAME;
import static eu.europeana.processing.job.JobParamName.METIS_XSLT_URL;
import static eu.europeana.processing.job.JobParamName.OAI_REPOSITORY_URL;
import static eu.europeana.processing.job.JobParamName.SET_SPEC;
import static eu.europeana.processing.job.JobParamName.VALIDATION_TYPE;

import eu.europeana.cloud.flink.client.RestJobExecutor;
import eu.europeana.processing.config.FlinkConfigurationProperties;
import eu.europeana.processing.config.JarIdsProperties;
import eu.europeana.processing.config.JobsConfigurationProperties;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.model.JobDetailsDto;
import eu.europeana.processing.model.JobSubmissionDto;
import eu.europeana.processing.model.TaskInfo;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;


@Log4j2
class OaiFlinkPerformanceForRestAppIT extends AbstractPerformanceIT {

  @Autowired
  private FlinkConfigurationProperties flinkConfigurationProperties;

  @Autowired
  protected JobsConfigurationProperties jobsConfigurationProperties;

  @Autowired
  protected JarIdsProperties jarIdsProperties;

  @Autowired
  private DbCleaner dbCleaner;

  @Test
  void step1_shouldExecuteHarvestCompletelyWithoutErrors() throws Exception {

    //given
    dbCleaner.clearDbFor(1);

    //when
    RestJobExecutor restJobExecutor = new RestJobExecutor();
    JobDetailsDto jobDetails = restJobExecutor.execute(
        new JobSubmissionDto(JobName.OAI_HARVEST,
            Map.of(
                OAI_REPOSITORY_URL,
                sourceProperties.getUrl(),
                DATASET_ID,
                "JUnitSmall",
                METADATA_PREFIX,
                sourceProperties.getMetadataPrefix(),
                SET_SPEC,
                sourceProperties.getSetSpec()
                ))
    );

    //then
    long processedRecords = executionRecordRepository.countByDatasetIdAndExecutionId("JUnitSmall", jobDetails.taskId() + "");
    Assertions.assertThat(processedRecords).isEqualTo(sourceProperties.getRecordCount());
  }


  private TaskInfo findPreviousTaskFor(String jobName){
    List<TaskInfo> previousTask = taskInfoRepository.findByTaskName(jobName, Limit.of(1));
    if (previousTask.size() != 1) {
      Assertions.fail("Previous task not found");
      return null;
    } else {
      log.info("Will execute test for taskId={} as source task.", previousTask.getFirst().getTaskId());
      return previousTask.getFirst();
    }
  }

  @Test
  void step2_shouldExecuteExternalValidationWithoutErrors() throws Exception {
    //given
    TaskInfo previousTask = findPreviousTaskFor(JobName.OAI_HARVEST);

    //when
    RestJobExecutor restJobExecutor = new RestJobExecutor();
    JobDetailsDto jobDetails = restJobExecutor.execute(
        new JobSubmissionDto(JobName.VALIDATION_EXTERNAL,
            Map.of(
                DATASET_ID,
                "JUnitSmall",
                EXECUTION_ID,
                previousTask.getTaskId()+"",
                VALIDATION_TYPE,
                "VALIDATION_EXTERNAL"
            ))
    );

    //then
    long processedRecords = executionRecordRepository.countByDatasetIdAndExecutionId("JUnitSmall", jobDetails.taskId() + "");
    Assertions.assertThat(processedRecords).isEqualTo(sourceProperties.getRecordCount());
  }

  @Test
  void step3_shouldExecuteXsltTransformationWithoutErrors() throws Exception {
    //given
    TaskInfo previousTask = findPreviousTaskFor(JobName.VALIDATION_EXTERNAL);

    //when
    RestJobExecutor restJobExecutor = new RestJobExecutor();
    JobDetailsDto jobDetails = restJobExecutor.execute(
        new JobSubmissionDto(JobName.TRANSFORMATION,
            Map.of(
                DATASET_ID,
                "JUnitSmall",
                EXECUTION_ID,
                previousTask.getTaskId()+"",
                METIS_DATASET_NAME, "idA_metisDatasetNameA",
                METIS_DATASET_COUNTRY, "Greece",
                METIS_DATASET_LANGUAGE, "el",
                METIS_XSLT_URL, "https://metis-core-rest.test.eanadev.org/datasets/xslt/6204e5e2514e773e6745f7e9"
            ))
    );
    //then
    long processedRecords = executionRecordRepository.countByDatasetIdAndExecutionId("JUnitSmall", jobDetails.taskId() + "");
    Assertions.assertThat(processedRecords).isEqualTo(sourceProperties.getRecordCount());
  }

  @Test
  void step4_shouldExecuteInternalValidationWithoutErrors() throws Exception {
    //given
    TaskInfo previousTask = findPreviousTaskFor(JobName.TRANSFORMATION);

    //when
    RestJobExecutor restJobExecutor = new RestJobExecutor();
    JobDetailsDto jobDetails = restJobExecutor.execute(
        new JobSubmissionDto(JobName.VALIDATION_INTERNAL,
            Map.of(
                DATASET_ID,
                "JUnitSmall",
                EXECUTION_ID,
                previousTask.getTaskId()+"",
                VALIDATION_TYPE,
                "VALIDATION_INTERNAL"
            ))
    );
    //then
    long processedRecords = executionRecordRepository.countByDatasetIdAndExecutionId("JUnitSmall", jobDetails.taskId() + "");
    Assertions.assertThat(processedRecords).isEqualTo(sourceProperties.getRecordCount());
  }

  @Test
  void step5_shouldExecuteNormalizationWithoutErrors() throws Exception {
    //given
    TaskInfo previousTask = findPreviousTaskFor(JobName.VALIDATION_INTERNAL);

    //when
    RestJobExecutor restJobExecutor = new RestJobExecutor();
    JobDetailsDto jobDetails = restJobExecutor.execute(
        new JobSubmissionDto(JobName.NORMALIZATION,
            Map.of(
                DATASET_ID,
                "JUnitSmall",
                EXECUTION_ID,
                previousTask.getTaskId()+""
            ))
    );
    //then
    long processedRecords = executionRecordRepository.countByDatasetIdAndExecutionId("JUnitSmall", jobDetails.taskId() + "");
    Assertions.assertThat(processedRecords).isEqualTo(sourceProperties.getRecordCount());
  }

  @Test
  void step6_shouldExecuteEnrichmentWithoutErrors() throws Exception {
    //given
    TaskInfo previousTask = findPreviousTaskFor(JobName.NORMALIZATION);

    //when
    RestJobExecutor restJobExecutor = new RestJobExecutor();
    JobDetailsDto jobDetails = restJobExecutor.execute(
        new JobSubmissionDto(JobName.ENRICHMENT,
            Map.of(
                DATASET_ID,
                "JUnitSmall",
                EXECUTION_ID,
                previousTask.getTaskId()+"",
                ENRICHMENT_ENTITY_MANAGEMENT_URL, jobsConfigurationProperties.getEnrichment().getEntityManagementUrl(),
                ENRICHMENT_ENTITY_API_URL, jobsConfigurationProperties.getEnrichment().getEntityApiUrl(),
                ENRICHMENT_ENTITY_API_TOKEN_ENDPOINT, jobsConfigurationProperties.getEnrichment().getEntityApiTokenEndpoint(),
                ENRICHMENT_ENTITY_API_GRANT_PARAMS, jobsConfigurationProperties.getEnrichment().getEntityApiGrantParams()
            ))
    );
    //then
    long processedRecords = executionRecordRepository.countByDatasetIdAndExecutionId("JUnitSmall", jobDetails.taskId() + "");
    Assertions.assertThat(processedRecords).isEqualTo(sourceProperties.getRecordCount());
  }

  @Test
  void step7_shouldExecuteMediaWithoutErrors() throws Exception {
    //given
    TaskInfo previousTask = findPreviousTaskFor(JobName.ENRICHMENT);

    //when
    RestJobExecutor restJobExecutor = new RestJobExecutor();
    JobDetailsDto jobDetails = restJobExecutor.execute(
        new JobSubmissionDto(JobName.MEDIA,
            Map.of(
                DATASET_ID,
                "JUnitSmall",
                EXECUTION_ID,
                previousTask.getTaskId()+""
            ))
    );
    //then
    long processedRecords = executionRecordRepository.countByDatasetIdAndExecutionId("JUnitSmall", jobDetails.taskId() + "");
    Assertions.assertThat(processedRecords).isEqualTo(sourceProperties.getRecordCount());
  }

  @Test
  void step8_shouldExecuteIndexingWithoutErrors() throws Exception {
    //given
    TaskInfo previousTask = findPreviousTaskFor(JobName.MEDIA);

    Map<String, String> taskParameters = new HashMap<>();
    taskParameters.put(DATASET_ID, "JUnitSmall");
    taskParameters.put(EXECUTION_ID, previousTask.getTaskId() + "");
    taskParameters.put(INDEXING_PRESERVETIMESTAMPS, jobsConfigurationProperties.getIndexing().getPreserveTimestamps());
    taskParameters.put(INDEXING_PERFORMREDIRECTS, jobsConfigurationProperties.getIndexing().getPerformRedirects());
    taskParameters.put(INDEXING_MONGOINSTANCES, jobsConfigurationProperties.getIndexing().getMongoInstances());
    taskParameters.put(INDEXING_MONGOPORTNUMBER, jobsConfigurationProperties.getIndexing().getMongoPortNumber());
    taskParameters.put(INDEXING_MONGODBNAME, jobsConfigurationProperties.getIndexing().getMongoDbName());
    taskParameters.put(INDEXING_MONGOREDIRECTDBNAME, jobsConfigurationProperties.getIndexing().getMongoRedirectsDbName());
    taskParameters.put(INDEXING_MONGOUSERNAME, jobsConfigurationProperties.getIndexing().getMongoUsername());
    taskParameters.put(INDEXING_MONGOPASSWORD, jobsConfigurationProperties.getIndexing().getMongoPassword());
    taskParameters.put(INDEXING_MONGOAUTHDB, jobsConfigurationProperties.getIndexing().getMongoAuthDB());
    taskParameters.put(INDEXING_MONGOUSESSL, jobsConfigurationProperties.getIndexing().getMongoUseSSL());
    taskParameters.put(INDEXING_MONGOREADPREFERENCE, jobsConfigurationProperties.getIndexing().getMongoReadPreference());
    taskParameters.put(INDEXING_MONGOPOOLSIZE, jobsConfigurationProperties.getIndexing().getMongoPoolSize());
    taskParameters.put(INDEXING_SOLRINSTANCES, jobsConfigurationProperties.getIndexing().getMongoApplicationName());
    taskParameters.put(INDEXING_ZOOKEEPERINSTANCES, jobsConfigurationProperties.getIndexing().getZookeeperInstances());
    taskParameters.put(INDEXING_ZOOKEEPERPORTNUMBER, jobsConfigurationProperties.getIndexing().getZookeeperPortNumber());
    taskParameters.put(INDEXING_ZOOKEEPERCHROOT, jobsConfigurationProperties.getIndexing().getZookeeperChroot());
    taskParameters.put(INDEXING_ZOOKEEPERDEFAULTCOLLECTION,
        jobsConfigurationProperties.getIndexing().getZookeeperDefaultCollection());
    taskParameters.put(INDEXING_MONGOAPPLICATIONNAME,
        jobsConfigurationProperties.getIndexing().getZookeeperDefaultCollection());


    //when
    RestJobExecutor restJobExecutor = new RestJobExecutor();
    JobDetailsDto jobDetails = restJobExecutor.execute(
        new JobSubmissionDto(JobName.MEDIA,
            taskParameters
        )
    );
    //then
    long processedRecords = executionRecordRepository.countByDatasetIdAndExecutionId("JUnitSmall", jobDetails.taskId() + "");
    Assertions.assertThat(processedRecords).isEqualTo(sourceProperties.getRecordCount());
  }
}

