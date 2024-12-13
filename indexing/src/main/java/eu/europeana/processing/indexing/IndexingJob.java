package eu.europeana.processing.indexing;

import eu.europeana.processing.MetisJob;
import eu.europeana.processing.indexing.processor.IndexingOperator;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.indexing.validation.IndexingJobParamValidator;
import eu.europeana.processing.validation.JobParamValidator;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexingJob extends MetisJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexingJob.class);

    protected IndexingJob(String[] args) {
        super(args, JobName.INDEXING);
    }

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
