package eu.europeana.processing.normalization.processor;

import eu.europeana.normalization.Normalizer;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.normalization.model.NormalizationResult;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizationOperator extends ProcessFunction<ExecutionRecord, ExecutionRecordResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NormalizationOperator.class);

    private transient NormalizerFactory normalizerFactory;
    private ParameterTool parameterTool;

    @Override
    public void open(Configuration parameters) throws Exception {
        normalizerFactory = new NormalizerFactory();
        parameterTool = ParameterTool.fromMap(getRuntimeContext().getExecutionConfig().getGlobalJobParameters().toMap());
        LOGGER.info("Created normalization operator.");
    }


    @Override
    public void processElement(ExecutionRecord sourceExecutionRecord, ProcessFunction<ExecutionRecord, ExecutionRecordResult>.Context ctx, Collector<ExecutionRecordResult> out) throws Exception {

        final Normalizer normalizer = normalizerFactory.getNormalizer();

        NormalizationResult normalizationResult = normalizer.normalize(sourceExecutionRecord.getRecordData());
        if (normalizationResult.getErrorMessage() == null) {
            out.collect(
                    ExecutionRecordResult.from(
                            sourceExecutionRecord,
                            parameterTool.get(JobParamName.TASK_ID),
                            JobName.NORMALIZATION,
                            normalizationResult.getNormalizedRecordInEdmXml(),
                            null)
            );
        } else {
            out.collect(
                    ExecutionRecordResult.from(
                            sourceExecutionRecord,
                            parameterTool.get(JobParamName.TASK_ID),
                            JobName.NORMALIZATION,
                            "",
                            normalizationResult.getErrorMessage())
            );
        }
    }
}
