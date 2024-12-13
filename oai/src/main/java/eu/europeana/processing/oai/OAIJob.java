package eu.europeana.processing.oai;

import eu.europeana.processing.MetisJob;
import eu.europeana.processing.oai.processor.DeletedRecordFilter;
import eu.europeana.processing.oai.processor.IdAssigningOperator;
import eu.europeana.processing.oai.processor.RecordHarvestingOperator;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.sink.DbSinkFunction;
import eu.europeana.processing.oai.reader.OAIHeadersSource;
import eu.europeana.processing.validation.JobParamValidator;
import eu.europeana.processing.oai.validation.OAIJobParamValidator;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAIJob extends MetisJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAIJob.class);

    protected OAIJob(String[] args) {
        super(args, JobName.OAI_HARVEST);
    }

    protected StreamExecutionEnvironment prepareEnvironment() {
        final StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);
        generateTaskIdIfNeeded();
        env.getConfig().setGlobalJobParameters(tool);
        env.getCheckpointConfig().disableCheckpointing();
        return env;
    }


    @Override
    protected void prepareJob() {
        flinkEnvironment.fromSource(
            new OAIHeadersSource(tool), WatermarkStrategy.noWatermarks(), createSourceName()).setParallelism(1)

        .filter(new DeletedRecordFilter()).setParallelism(operatorParallelism)
        .process(new RecordHarvestingOperator(tool)).setParallelism(operatorParallelism)
        .process(new IdAssigningOperator()).setParallelism(operatorParallelism)
                        .addSink(new DbSinkFunction()).setParallelism(sinkParallelism);
    }

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting {}...", OAIJob.class.getSimpleName());
        new OAIJob(args).execute();
    }

    private String createSourceName() {
        return "OAI (url: " + tool.get(JobParamName.OAI_REPOSITORY_URL)
            + ", set: " + tool.get(JobParamName.SET_SPEC) +
            ", format: " + tool.get(JobParamName.METADATA_PREFIX) + ")";
    }

    public ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator(){
        throw new UnsupportedOperationException();
    }

    @Override
    public JobParamValidator getParamValidator() {
        return new OAIJobParamValidator();
    }
}
