package eu.europeana.processing.normalization;

import eu.europeana.processing.MetisJob;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.normalization.processor.NormalizationOperator;
import eu.europeana.processing.validation.JobParamValidator;
import eu.europeana.processing.normalization.validation.NormalizationJobParamValidator;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p><b>How to run the job</b></p>
 * <p>All required parameters have to be provided in application args. In case of this job we have the following (example) arguments:
 *
 * <p>
 *     <ul>--datasetId 12</ul>
 *     <ul>--executionId 261</ul>
 *     <ul>--chunkSize 12</ul>
 *     <ul>--datasource.url jdbc:postgresql://localhost:5432/spring-batch-metis-poc</ul>
 *     <ul>--datasource.username admin</ul>
 *     <ul>--datasource.password admin</ul>
 * </p>
 */
public class NormalizationJob extends MetisJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(NormalizationJob.class);

    protected NormalizationJob(String[] args) {
        super(args, JobName.NORMALIZATION);
    }

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting {}...", NormalizationJob.class.getSimpleName());
        new NormalizationJob(args).execute();
    }

    @Override
    public ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator() {
        return new NormalizationOperator();
    }

    @Override
    public JobParamValidator getParamValidator() {
        return new NormalizationJobParamValidator();
    }
}
