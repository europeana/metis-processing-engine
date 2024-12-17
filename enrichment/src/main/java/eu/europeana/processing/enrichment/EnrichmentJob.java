package eu.europeana.processing.enrichment;

import eu.europeana.processing.MetisJob;
import eu.europeana.processing.enrichment.processor.EnrichmentOperator;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.enrichment.validation.EnrichmentJobParamValidator;
import eu.europeana.processing.validation.JobParamValidator;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p><b>General description:</b></p>
 * <p>Entry point class for <u>Enrichment Job</u> that defines the flow of the execution.</p>
 *
 * <p>Enrichment job consists of the following components.</p>
 * <ul>
 *   <li>source defined in {@link eu.europeana.processing.source.DbSourceWithProgressHandling}</li>
 *   <li>operator responsible for enriching the {@link ExecutionRecord} instances defined in {@link EnrichmentOperator}</li>
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
 *  <li>validationType</li>
 *  <li>datasource.url</li>
 *  <li>datasource.username</li>
 *  <li>datasource.password</li>
 *  <li>dereferenceUrl</li>
 *  <li>entityManagementUrl</li>
 *  <li>entityApiUrl</li>
 *  <li>entityApiKey</li>
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
 *  --dereferenceUrl https://dereference.url
 *  --entityManagementUrl https://dereference.url/entity
 *  --entityApiUrl https://dereference.url/entity
 *  --entityApiKey apiKey
 * </blockquote>
 */
public class EnrichmentJob extends MetisJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichmentJob.class);

    protected EnrichmentJob(String[] args) {
        super(args, JobName.ENRICHMENT);
    }

    /**
     * Entry point for job
     *
     * @param args list of all required and optional arguments for job
     * @throws Exception in case of any Exception
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting {}...", EnrichmentJob.class.getSimpleName());
        new EnrichmentJob(args).execute();
    }

    @Override
    public ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator() {
        return new EnrichmentOperator();
    }

    @Override
    public JobParamValidator getParamValidator() {
        return new EnrichmentJobParamValidator();
    }
}
