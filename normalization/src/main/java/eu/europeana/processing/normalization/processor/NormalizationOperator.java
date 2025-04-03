package eu.europeana.processing.normalization.processor;

import eu.europeana.normalization.Normalizer;
import eu.europeana.normalization.NormalizerFactory;
import eu.europeana.normalization.model.NormalizationResult;
import eu.europeana.normalization.util.NormalizationException;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import java.io.Serial;

import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.util.ParameterTool;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Main operator for {@link eu.europeana.processing.normalization.NormalizationJob}.</p>
 * <p>It uses Metis provided {@link Normalizer}</p>
 */
public class NormalizationOperator extends ProcessFunction<ExecutionRecord, ExecutionRecordResult> {

    @Serial
    private static final long serialVersionUID = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(NormalizationOperator.class);

    private transient Normalizer normalizer;
    private ParameterTool parameterTool;

    @Override
    public void open(OpenContext openContext) throws Exception {
        NormalizerFactory normalizerFactory = new NormalizerFactory();
        parameterTool = ParameterTool.fromMap(getRuntimeContext().getGlobalJobParameters());
        normalizer = normalizerFactory.getNormalizer();
        LOGGER.info("Created normalization operator.");
    }


    @Override
    public void processElement(
        ExecutionRecord sourceExecutionRecord,
        ProcessFunction<ExecutionRecord, ExecutionRecordResult>.Context ctx,
        Collector<ExecutionRecordResult> out) {
        try {
            normalizeRecord(sourceExecutionRecord, out);
        } catch(NormalizationException e){
            LOGGER.warn("During process of normalization of record with id: {}, Exception was caught", sourceExecutionRecord.getExecutionRecordKey().getRecordId(), e);
            out.collect(
                    ExecutionRecordResult.from(
                            sourceExecutionRecord,
                            parameterTool.get(JobParamName.TASK_ID),
                            JobName.NORMALIZATION,
                            ExecutionRecord.EMPTY,
                            e.getMessage())
            );
        }
    }

    private void normalizeRecord(ExecutionRecord sourceExecutionRecord, Collector<ExecutionRecordResult> out) throws NormalizationException {
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
                            ExecutionRecord.EMPTY,
                            normalizationResult.getErrorMessage())
            );
        }
    }
}
