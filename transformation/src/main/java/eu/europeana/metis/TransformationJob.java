package eu.europeana.metis;


import eu.europeana.processing.MetisJob;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Transformation job that uses checkpoints and reader blockage.</p>
 *
 * <p><b>How to run the job</b></p>
 * <p>All required parameters have to be provided in application args. In case of this job we have the following (example) arguments:
 *
 * <p>
 *     <ul>--datasetId 12</ul>
 *     <ul>--executionId 261</ul>
 *     <ul>--chunkSize 12</ul>
 *     <ul>--metisDatasetName idA_metisDatasetNameA</ul>
 *     <ul>--metisDatasetCountry Greece</ul>
 *     <ul>--metisDatasetLanguage el</ul>
 *     <ul>--metisXsltUrl https://metis-core-rest.test.eanadev.org/datasets/xslt/6204e5e2514e773e6745f7e9</ul>
 *     <ul>--datasource.url jdbc:postgresql://localhost:5432/spring-batch-metis-poc</ul>
 *     <ul>--datasource.username admin</ul>
 *     <ul>--datasource.password admin</ul>
 * </p>
 *
 */
public class TransformationJob extends MetisJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformationJob.class);

    protected TransformationJob(String[] args) {
        super(args, JobName.TRANSFORMATION);
    }

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting {}...", TransformationJob.class.getSimpleName());
        new TransformationJob(args).execute();
    }

    @Override
    public ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator() {
        return new TransformationOperator();
    }
}
