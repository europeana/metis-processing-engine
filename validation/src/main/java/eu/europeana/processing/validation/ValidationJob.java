package eu.europeana.processing.validation;

import eu.europeana.processing.MetisJob;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.validation.processor.ValidationOperator;
import eu.europeana.processing.validation.validation.ValidationJobParamValidator;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p><b>General description:</b></p>
 * <p>Entry point class for validation job that defines the flow of the execution.</p>
 *
 * <p>Validation job consists of the following components.</p>
 * <ul>
 *   <li>source defined in {@link eu.europeana.processing.source.DbSourceWithProgressHandling}</li>
 *   <li>operator responsible for validating the {@link ExecutionRecord} instances defined in {@link ValidationOperator}</li>
 *   <li>sink defined in {@link eu.europeana.processing.sink.DbSinkFunction}</li>
 * </ul>
 *
 * <p><b>How to run the job</b></p>
 * <p>Job can be executed by starting main method with all needed arguments</p>
 * <p>The following args are required:</p>
 *
 *<ul style="list-style: none">
 *  <li>datasetId</li>
 *  <li>executionId</li>
 *  <li>validationType</li>
 *  <li>datasource.url</li>
 *  <li>datasource.username</li>
 *  <li>datasource.password</li>
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
 *  --validationType VALIDATION_EXTERNAL
 *  --datasource.url jdbc:postgresql://localhost:5432/spring-batch-metis-poc
 *  --datasource.username admin
 *  --datasource.password admin
 *  --chunkSize 12
 * </blockquote>
 */
public class ValidationJob extends MetisJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationJob.class);

    protected ValidationJob(String[] args) {
        super(args, ParameterTool.fromArgs(args).getRequired(JobParamName.VALIDATION_TYPE));
    }

    /**
     * Entry point for job
     *
     * @param args list of all required and optional arguments for job
     * @throws Exception in case of any Exception
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting {}...", ValidationJob.class.getSimpleName());
        new ValidationJob(args).execute();
    }

    @Override
    public ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator() {
        return new ValidationOperator();
    }

    @Override
    public JobParamValidator getParamValidator() {
        return new ValidationJobParamValidator();
    }
}
