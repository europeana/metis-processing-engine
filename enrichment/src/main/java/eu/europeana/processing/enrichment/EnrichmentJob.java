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

public class EnrichmentJob extends MetisJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichmentJob.class);

    protected EnrichmentJob(String[] args) {
        super(args, JobName.ENRICHMENT);
    }

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
