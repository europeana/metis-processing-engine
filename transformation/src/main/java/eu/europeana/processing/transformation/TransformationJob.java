package eu.europeana.processing.transformation;


import eu.europeana.processing.MetisJob;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.transformation.processor.TransformationOperator;
import eu.europeana.processing.validation.JobParamValidator;
import eu.europeana.processing.transformation.validation.TransformationJobParamValidator;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p><b>General description:</b></p>
 * <p>Entry point class for <u>Transformation Job</u> that defines the flow of the execution.</p>
 *
 * <p>Indexing job consists of the following components.</p>
 * <ul>
 *   <li>source defined in {@link eu.europeana.processing.source.DbSourceWithProgressHandling}</li>
 *   <li>operator responsible for transforming the {@link ExecutionRecord} instances defined in {@link eu.europeana.processing.transformation.processor.TransformationOperator}</li>
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
 *  <li>metisDatasetName</li>
 *  <li>metisDatasetCountry</li>
 *  <li>metisDatasetLanguage</li>
 *  <li>metisXsltUrl</li>
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
 *  --metisDatasetName idA_metisDatasetNameA
 *  --metisDatasetCountry Greece
 *  --metisDatasetLanguage el
 *  --metisXsltUrl https://url.rurl
 * </blockquote>
 */
public class TransformationJob extends MetisJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformationJob.class);

    protected TransformationJob(String[] args) {
        super(args, JobName.TRANSFORMATION);
    }

    /**
     * Entry point for job
     *
     * @param args list of all required and optional arguments for job
     * @throws Exception in case of any Exception
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting {}...", TransformationJob.class.getSimpleName());
        new TransformationJob(args).execute();
    }

    @Override
    public ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator() {
        return new TransformationOperator();
    }

    @Override
    public JobParamValidator getParamValidator() {
        return new TransformationJobParamValidator();
    }
}
