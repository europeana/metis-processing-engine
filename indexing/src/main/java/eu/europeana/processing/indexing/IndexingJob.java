package eu.europeana.processing.indexing;

import eu.europeana.processing.MetisJob;
import eu.europeana.processing.indexing.processor.IndexingOperator;
import eu.europeana.processing.indexing.validation.IndexingJobParamValidator;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.validation.JobParamValidator;
import java.util.Set;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p><b>General description:</b></p>
 * <p>Entry point class for <u>Indexing Job</u> that defines the flow of the execution.</p>
 *
 * <p>Indexing job consists of the following components.</p>
 * <ul>
 *   <li>source defined in {@link eu.europeana.processing.source.DbSourceWithProgressHandling}</li>
 *   <li>operator responsible for indexing the {@link ExecutionRecord} instances defined in {@link IndexingOperator}</li>
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
 *  <li>preserveTimestamps/li>
 *  <li>performRedirects</li>
 *  <li>mongoInstances</li>
 *  <li>mongoPortNumber</li>
 *  <li>mongoDbName</li>
 *  <li>mongoRedirectsDbName</li>
 *  <li>mongoUsername</li>
 *  <li>mongoPassword</li>
 *  <li>mongoAuthDB</li>
 *  <li>mongoUseSSL</li>
 *  <li>mongoReadPreference</li>
 *  <li>mongoPoolSize</li>
 *  <li>solrInstances</li>
 *  <li>zookeeperInstances</li>
 *  <li>zookeeperPortNumber</li>
 *  <li>zookeeperChroot</li>
 *  <li>zookeeperDefaultCollection</li>
 *  <li>mongoApplicationName</li>
 *</ul>
 *
 * <p>The following args are optional:</p>
 * <ul>
 *  <li>chunkSize</li>
 * </ul>
 *
 */
public class IndexingJob extends MetisJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexingJob.class);

    protected IndexingJob(String[] args) {
        super(args, JobName.INDEXING);
    }

    /**
     * Entry point for job
     *
     * @param args list of all required and optional arguments for job
     * @throws Exception in case of any Exception
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting {}...", IndexingJob.class.getSimpleName());
        new IndexingJob(args).execute();
    }

    @Override
    public ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator() {
        return new IndexingOperator();
    }

    @Override
    public JobParamValidator getParamValidator() {
        return new IndexingJobParamValidator();
    }

}
