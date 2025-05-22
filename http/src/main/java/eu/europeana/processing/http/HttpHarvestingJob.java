package eu.europeana.processing.http;

import eu.europeana.processing.MetisJob;
import eu.europeana.processing.harvesting.processor.IdAssigningOperator;
import eu.europeana.processing.http.reader.HttpSource;
import eu.europeana.processing.http.validation.HttpJobParamValidator;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;

import eu.europeana.processing.sink.DbSink;
import eu.europeana.processing.validation.JobParamValidator;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p><b>General description:</b></p>
 * <p>Entry point class for <u>Http harvesting job</u> that defines the flow of the execution.</p>
 *
 * <p>Http harvesting job consists of the following components.</p>
 * <ul>
 *   <li>source responsible for downloading archive file from http url and providing content of the files contained in it.
 *   Defined in {@link HttpSource}</li>
 *   <li>operator responsible for assigning identifiers to the records {@link IdAssigningOperator}</li>
 *   <li>sink defined in {@link eu.europeana.processing.sink.DbSink}</li>
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
 *  <li>httpArchiveUrl</li>
 *</ul>
 *
 * <p>The following args are optional:</p>
 * <ul>
 *  <li>chunkSize</li>
 * </ul>
 *
 *
 * <p>Example list of arguments for specific execution may be defined in the following way:</p>
 * <blockquote>
 *  --datasetId 12
 *  --executionId 261
 *  --datasource.url jdbc:postgresql://localhost:5432/spring-batch-metis-poc
 *  --datasource.username admin
 *  --datasource.password admin
 *  --chunkSize 12
 *  --httpArchiveUrl=http://ftp.eanadev.org/uploads/Kulturpool_new.zip
 * </blockquote>
 */
public class HttpHarvestingJob extends MetisJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpHarvestingJob.class);

    protected HttpHarvestingJob(String[] args) {
        super(args, JobName.HTTP_HARVEST);
    }

    @Override
    protected void prepareJob() {
        flinkEnvironment.fromSource(
            new HttpSource(tool), WatermarkStrategy.noWatermarks(), createHttpSourceName()).setParallelism(readerParallelism)
                        .process(new IdAssigningOperator()).setParallelism(operatorParallelism)
                        .sinkTo(new DbSink(tool)).setParallelism(sinkParallelism);
    }

    /**
     * Entry point for job
     *
     * @param args list of all required and optional arguments for job
     * @throws Exception in case of any Exception
     */
    @SuppressWarnings("java:S2096") //The Flink engine on the cluster is responsible for handling exception
    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting {}...", HttpHarvestingJob.class.getSimpleName());
        new HttpHarvestingJob(args).execute();
    }

    private String createHttpSourceName() {
        return "HTTP (url: " + tool.get(JobParamName.HTTP_ARCHIVE_URL) + ")";
    }

    public ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator(){
        throw new UnsupportedOperationException();
    }

    @Override
    public JobParamValidator getParamValidator() {
        return new HttpJobParamValidator();
    }
}
