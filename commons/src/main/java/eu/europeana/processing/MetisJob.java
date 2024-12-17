package eu.europeana.processing;

import eu.europeana.processing.job.JobParam;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.repository.TaskInfoRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import eu.europeana.processing.sink.DbSinkFunction;
import eu.europeana.processing.source.DbSourceWithProgressHandling;
import eu.europeana.processing.validation.JobParamValidator;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;

import java.util.Map;
import java.util.Random;

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
        readerParallelism = tool.getInt(JobParamName.READER_PARALLELISM, JobParam.DEFAULT_READER_PARALLELISM);
        operatorParallelism = tool.getInt(JobParamName.OPERATOR_PARALLELISM, JobParam.DEFAULT_OPERATOR_PARALLELISM);
        sinkParallelism = tool.getInt(JobParamName.SINK_PARALLELISM, JobParam.DEFAULT_SINK_PARALLELISM);
        flinkEnvironment = prepareEnvironment();
    }

    protected StreamExecutionEnvironment prepareEnvironment() {
        final StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);
        generateTaskIdIfNeeded();
        env.getConfig().setGlobalJobParameters(tool);
        env.enableCheckpointing(CHECKPOINT_INTERVAL_IN_MILLIS);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(MIN_PAUSE_BETWEEN_CHECKPOINTS);
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
            throw new RuntimeException("Error while generating task id!",e);
        }
    }

    protected void prepareJob() {
        flinkEnvironment
            .fromSource(new DbSourceWithProgressHandling(tool), WatermarkStrategy.noWatermarks(), createSourceName())
            .setParallelism(readerParallelism)
            .process(getMainOperator()).setParallelism(operatorParallelism)
            .addSink(new DbSinkFunction()).setParallelism(sinkParallelism);
    }

    /**
     * Executes the defined job
     *
     * @throws Exception in case of any failure during execution
     */
    public void execute() throws Exception {
        validateJobParams();
        prepareJob();
        flinkEnvironment.execute(enrichedJobName());
    }

    private String enrichedJobName() {
        return jobName + " (dataset: " + tool.get(JobParamName.DATASET_ID) + ", taskId: " + tool.get(JobParamName.TASK_ID) + ")";
    }

    private String createSourceName() {
        return "dbSource (dataset: " + tool.get(JobParamName.DATASET_ID) + ", executionId: " + tool.get(JobParamName.EXECUTION_ID) + ")";
    }


    public abstract ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator();

    public abstract JobParamValidator getParamValidator();

}
