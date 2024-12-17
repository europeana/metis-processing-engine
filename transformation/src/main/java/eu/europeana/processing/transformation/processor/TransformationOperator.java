package eu.europeana.processing.transformation.processor;

import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import eu.europeana.metis.transformation.service.TransformationException;
import eu.europeana.metis.transformation.service.XsltTransformer;
import eu.europeana.processing.job.JobName;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import java.io.Serial;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class TransformationOperator extends ProcessFunction<ExecutionRecord, ExecutionRecordResult> {

    @Serial
    private static final long serialVersionUID = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformationOperator.class);

    private ParameterTool parameterTool;

    @Override
    public void open(Configuration parameters) throws Exception {
        parameterTool = ParameterTool.fromMap(getRuntimeContext().getExecutionConfig().getGlobalJobParameters().toMap());
    }

    @Override
    public void processElement(ExecutionRecord sourceExecutionRecord, ProcessFunction<ExecutionRecord, ExecutionRecordResult>.Context ctx, Collector<ExecutionRecordResult> out) throws Exception {
        ExecutionRecordResult result;
        try {
            final XsltTransformer xsltTransformer = prepareXsltTransformer();

            StringWriter writer =
                    xsltTransformer.transform(
                            sourceExecutionRecord.getRecordData().getBytes(StandardCharsets.UTF_8),
                            prepareEuropeanaGeneratedIdsMap(sourceExecutionRecord));

            result = ExecutionRecordResult.from(
                sourceExecutionRecord,
                parameterTool.get(JobParamName.TASK_ID),
                JobName.TRANSFORMATION,
                writer.toString(),
                null);
            LOGGER.debug("Transformed record, id: {}", sourceExecutionRecord.getExecutionRecordKey().getRecordId());
        } catch (Exception e) {
            LOGGER.warn("{} exception: {}", getClass().getName(), sourceExecutionRecord.getExecutionRecordKey().getRecordId(), e);
            result = ExecutionRecordResult.from(
                sourceExecutionRecord,
                parameterTool.get(JobParamName.TASK_ID),
                JobName.TRANSFORMATION,
                "",
                e.getMessage());
        }

        out.collect(result);
    }

    private XsltTransformer prepareXsltTransformer()
            throws TransformationException {
        return new XsltTransformer(
                parameterTool.get(JobParamName.METIS_XSLT_URL),
                parameterTool.get(JobParamName.METIS_DATASET_NAME),
                parameterTool.get(JobParamName.METIS_DATASET_COUNTRY),
                parameterTool.get(JobParamName.METIS_DATASET_LANGUAGE));
    }

    private EuropeanaGeneratedIdsMap prepareEuropeanaGeneratedIdsMap(ExecutionRecord executionRecord)
            throws EuropeanaIdException {
        String metisDatasetId = parameterTool.get(JobParamName.DATASET_ID);
        //Prepare europeana identifiers
        EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = null;
        if (!StringUtils.isBlank(metisDatasetId)) {
            EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
            europeanaGeneratedIdsMap = europeanIdCreator
                    .constructEuropeanaId(executionRecord.getRecordData(), metisDatasetId);
        }
        return europeanaGeneratedIdsMap;
    }
}
