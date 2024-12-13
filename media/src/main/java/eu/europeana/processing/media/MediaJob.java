package eu.europeana.processing.media;


import eu.europeana.processing.MetisJob;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.media.processor.MediaOperator;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.validation.JobParamValidator;
import eu.europeana.processing.media.validation.MediaJobParamValidator;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaJob extends MetisJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaJob.class);

    protected MediaJob(String[] args) {
        super(args, JobName.MEDIA);
    }

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting {}...", MediaJob.class.getSimpleName());
        new MediaJob(args).execute();
    }

    @Override
    public ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator() {
        return new MediaOperator();
    }

    @Override
    public JobParamValidator getParamValidator() {
        return new MediaJobParamValidator();
    }

}
