package eu.europeana.processing.http;

import eu.europeana.processing.MetisJob;
import eu.europeana.processing.harvesting.processor.IdAssigningOperator;
import eu.europeana.processing.http.source.HttpSource;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;

import eu.europeana.processing.sink.DbSinkFunction;
import eu.europeana.processing.validation.HttpJobParamValidator;
import eu.europeana.processing.validation.JobParamValidator;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Http harvesting job. Harvests url with the archive file containing record files (XMLs) and stores
 * them in the PosgresDB.
 *
 */
public class HttpJob extends MetisJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpJob.class);

    protected HttpJob(String[] args) {
        super(args, JobName.HTTP_HARVEST);
    }

    @Override
    protected void prepareJob() {
        flinkEnvironment.fromSource(
            new HttpSource(tool), WatermarkStrategy.noWatermarks(), createSourceName()).setParallelism(readerParallelism)
                        .process(new IdAssigningOperator()).setParallelism(operatorParallelism)
                        .addSink(new DbSinkFunction()).setParallelism(sinkParallelism);
    }

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting {}...", HttpJob.class.getSimpleName());
        new HttpJob(args).execute();
    }

    private String createSourceName() {
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
