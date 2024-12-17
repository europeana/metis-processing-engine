package eu.europeana.processing.oai;

import eu.europeana.processing.MetisJob;
import eu.europeana.processing.oai.processor.DeletedRecordFilter;
import eu.europeana.processing.oai.processor.IdAssigningOperator;
import eu.europeana.processing.oai.processor.RecordHarvestingOperator;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.sink.DbSinkFunction;
import eu.europeana.processing.oai.reader.OAIHeadersSource;
import eu.europeana.processing.validation.JobParamValidator;
import eu.europeana.processing.oai.validation.OAIJobParamValidator;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p><b>General description:</b></p>
 * <p>Entry point class for <u>OAI Job</u> that defines the flow of the execution.</p>
 *
 * <p>Indexing job consists of the following components.</p>
 * <ul>
 *   <li>source defined in {@link OAIHeadersSource}</li>
 *   <li>operator responsible for downloading records from OAI source {@link RecordHarvestingOperator}</li>
 *   <li>operator responsible for assigning identifiers to the records {@link IdAssigningOperator}</li>
 *   <li>sink defined in {@link eu.europeana.processing.sink.DbSinkFunction}</li>
 * </ul>
 *
 * <p><b>How to run the job</b></p>
 * <p>Job can be executed by starting main method with all needed arguments</p>
 * <p>The following args are required:</p>
 *
 *<ul>
 *  <li>datasetId</li>
 *  <li>executionId</li>
 *  <li>datasource.url</li>
 *  <li>datasource.username</li>
 *  <li>datasource.password</li>
 *  <li>setSpec</li>
 *  <li>metadataPrefix</li>
 *  <li>oaiRepositoryUrl</li>
 *</ul>
 *
 * <p>The following args are optional:</p>
 * <ul>
 *  <li>chunkSize</li>
 * </ul>
 *
 *
 * <p>Example list of arguments for specific execution may in the following way:</p>
 * <blockquote>
 *  --datasetId 12
 *  --executionId 261
 *  --datasource.url jdbc:postgresql://localhost:5432/spring-batch-metis-poc
 *  --datasource.username admin
 *  --datasource.password admin
 *  --chunkSize 12
 *  --source.url=https://metis-repository-rest.test.eanadev.org/repository/oai
 *  --source.setSpec=Heide1000elements
 *  --source.metadataPrefix=edm
 * </blockquote>
 */
public class OAIJob extends MetisJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAIJob.class);

    protected OAIJob(String[] args) {
        super(args, JobName.OAI_HARVEST);
    }

    protected StreamExecutionEnvironment prepareEnvironment() {
        final StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);
        generateTaskIdIfNeeded();
        env.getConfig().setGlobalJobParameters(tool);
        env.getCheckpointConfig().disableCheckpointing();
        return env;
    }


    @Override
    protected void prepareJob() {
        flinkEnvironment.fromSource(
            new OAIHeadersSource(tool), WatermarkStrategy.noWatermarks(), createSourceName()).setParallelism(1)

        .filter(new DeletedRecordFilter()).setParallelism(operatorParallelism)
        .process(new RecordHarvestingOperator(tool)).setParallelism(operatorParallelism)
        .process(new IdAssigningOperator()).setParallelism(operatorParallelism)
                        .addSink(new DbSinkFunction()).setParallelism(sinkParallelism);
    }

    /**
     * Entry point for job
     *
     * @param args list of all required and optional arguments for job
     * @throws Exception in case of any Exception
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting {}...", OAIJob.class.getSimpleName());
        new OAIJob(args).execute();
    }

    private String createSourceName() {
        return "OAI (url: " + tool.get(JobParamName.OAI_REPOSITORY_URL)
            + ", set: " + tool.get(JobParamName.SET_SPEC) +
            ", format: " + tool.get(JobParamName.METADATA_PREFIX) + ")";
    }

    public ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator(){
        throw new UnsupportedOperationException();
    }

    @Override
    public JobParamValidator getParamValidator() {
        return new OAIJobParamValidator();
    }
}
