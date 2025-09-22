package eu.europeana.processing;

import eu.europeana.processing.job.JobParam;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.repository.TaskInfoRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import eu.europeana.processing.sink.DbSink;
import eu.europeana.processing.source.DbSourceWithProgressHandling;
import eu.europeana.processing.validation.JobParamValidator;
import java.time.Duration;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.util.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.configuration.RestartStrategyOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;

import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Main abstract class used by all the jobs executed by Metis.</p>
 * <p>Contains common methods for jobs. Responsible for:
 *  <li>preparing the job</li>
 *  <li>generating task identifier if needed</li>
 *  <li>triggering job arguments validation</li>
 *  <li>running the job</li>
 * </p>
 */
public abstract class MetisJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetisJob.class);

    ////////////////////////Failover strategy configuration/////////////////////////////////////////
    //All these gives us over 30 minutes of restarting in case of total infrastructure error. This
    // time is a bit random and depends on jitter and time of task starting.
    private static final Duration INITIAL_RESTART_DELAY_IN_SECONDS = Duration.ofSeconds(10);
    private static final Duration MAX_RESTART_DELAY_IN_SECONDS = Duration.ofMinutes(2);
    public static final double BACK_OFF_MULTIPLIER = 2.0;
    private static final int ATTEMPTS = 20;
    public static final double JITTER_FACTOR = 0.1;
    //After job works fine for configured time restart counter is reset.
    private static final Duration RESET_BACKOFF_THRESHOLD = Duration.ofMinutes(10);

    ////////////////////////Checkpointing configuration/////////////////////////////////////////////
    private static final long CHECKPOINT_INTERVAL_IN_MILLIS = 2000;
    private static final long MIN_PAUSE_BETWEEN_CHECKPOINTS = 1000;

    protected final StreamExecutionEnvironment flinkEnvironment;
    protected String jobName;
    protected ParameterTool tool;
    private final Random taskIdGenerator = new Random();
    protected final int readerParallelism;
    protected final int operatorParallelism;
    protected final int sinkParallelism;

    protected MetisJob(String[] args, String jobName) {
        this.jobName = jobName;
        tool = ParameterTool.fromArgs(args);
        validateJobParams();
        readerParallelism = tool.getInt(JobParamName.READER_PARALLELISM, JobParam.DEFAULT_READER_PARALLELISM);
        operatorParallelism = tool.getInt(JobParamName.OPERATOR_PARALLELISM, JobParam.DEFAULT_OPERATOR_PARALLELISM);
        sinkParallelism = tool.getInt(JobParamName.SINK_PARALLELISM, JobParam.DEFAULT_SINK_PARALLELISM);
        flinkEnvironment = prepareEnvironment();
    }

    protected StreamExecutionEnvironment prepareEnvironment() {

        Configuration config = new Configuration();
        config.set(RestartStrategyOptions.RESTART_STRATEGY, "exponential-delay");
        config.set(RestartStrategyOptions.RESTART_STRATEGY_EXPONENTIAL_DELAY_INITIAL_BACKOFF, INITIAL_RESTART_DELAY_IN_SECONDS);
        config.set(RestartStrategyOptions.RESTART_STRATEGY_EXPONENTIAL_DELAY_MAX_BACKOFF, MAX_RESTART_DELAY_IN_SECONDS);
        config.set(RestartStrategyOptions.RESTART_STRATEGY_EXPONENTIAL_DELAY_BACKOFF_MULTIPLIER, BACK_OFF_MULTIPLIER);
        config.set(RestartStrategyOptions.RESTART_STRATEGY_EXPONENTIAL_DELAY_ATTEMPTS, ATTEMPTS);
        config.set(RestartStrategyOptions.RESTART_STRATEGY_EXPONENTIAL_DELAY_RESET_BACKOFF_THRESHOLD, RESET_BACKOFF_THRESHOLD);
        config.set(RestartStrategyOptions.RESTART_STRATEGY_EXPONENTIAL_DELAY_JITTER_FACTOR, JITTER_FACTOR);

        final StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment(config);

        env.setParallelism(1);
        generateTaskIdIfNeeded();
        env.getConfig().setGlobalJobParameters(tool);
        env.enableCheckpointing(CHECKPOINT_INTERVAL_IN_MILLIS);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(MIN_PAUSE_BETWEEN_CHECKPOINTS);
        env.getCheckpointConfig().setExternalizedCheckpointRetention(ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION);
        return env;
    }

    protected void validateJobParams() {
        getParamValidator().validate(tool);
    }

    protected void generateTaskIdIfNeeded() {
        try (DbConnectionProvider dbConnectionProvider = new DbConnectionProvider(tool)) {
            TaskInfoRepository taskInfoRepository =
                RetryableMethodExecutor.createRetryProxy(new TaskInfoRepository(dbConnectionProvider));

            if (tool.get(JobParamName.TASK_ID) == null) {
                long taskId = taskIdGenerator.nextLong();
                taskInfoRepository.save(new TaskInfo(taskId, 0L, 0L));
                tool = tool.mergeWith(ParameterTool.fromMap(Map.of(JobParamName.TASK_ID, taskId + "")));
            } else {
                long taskId = tool.getLong(JobParamName.TASK_ID);
                if (taskInfoRepository.findById(taskId).isEmpty()) {
                    taskInfoRepository.save(new TaskInfo(taskId, 0L, 0L));
                }
            }
        }catch (Exception e){
            throw new RuntimeException("Error while generating task id!", e);
        }
    }

    protected void prepareJob() {
        flinkEnvironment
            .fromSource(new DbSourceWithProgressHandling(tool), WatermarkStrategy.noWatermarks(), createSourceName())
            .setParallelism(readerParallelism)
            .process(getMainOperator()).setParallelism(operatorParallelism)
            .sinkTo(new DbSink(tool)).setParallelism(sinkParallelism);
    }

    /**
     * Executes the defined job
     *
     * @throws Exception in case of any failure during execution
     */
    public void execute() throws Exception {
        prepareJob();
        LOGGER.info("Execution plan: {}", flinkEnvironment.getExecutionPlan());
        flinkEnvironment.execute(enrichedJobName());
    }

    private String enrichedJobName() {
        return jobName + " (dataset: " + tool.get(JobParamName.DATASET_ID) + ", taskId: " + tool.get(JobParamName.TASK_ID) + ")";
    }

    private String createSourceName() {
        return "dbSource (dataset: " + tool.get(JobParamName.DATASET_ID) + ", executionId: " + tool.get(JobParamName.EXECUTION_ID) + ")";
    }

    protected abstract ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator();

    public abstract JobParamValidator getParamValidator();

}
