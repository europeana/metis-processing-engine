package eu.europeana.processing.indexing.processor;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexerFactory;
import eu.europeana.indexing.IndexingSettings;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.processing.indexing.tool.IndexingSettingsGenerator;
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

import java.util.Collections;
import java.util.Date;

public class IndexingOperator extends ProcessFunction<ExecutionRecord, ExecutionRecordResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexingOperator.class);
    private transient IndexingSettings indexingSettings;
    private Date recordDate;
    private boolean preserveTimestamps;
    private boolean performRedirect;
    private ParameterTool parameterTool;
    private long taskId;

    @Override
    public void open(Configuration parameters) throws Exception {
        parameterTool = ParameterTool.fromMap(getRuntimeContext().getExecutionConfig().getGlobalJobParameters().toMap());
        taskId = parameterTool.getLong(JobParamName.TASK_ID);
        indexingSettings = prepareIndexingSetting(parameterTool);
        recordDate = new Date();
        preserveTimestamps = parameterTool.getBoolean(JobParamName.INDEXING_PRESERVETIMESTAMPS);
        performRedirect = parameterTool.getBoolean(JobParamName.INDEXING_PERFORMREDIRECTS);
    }

    private IndexingSettings prepareIndexingSetting(ParameterTool parameterTool) throws IndexingException {
       return new IndexingSettingsGenerator(parameterTool).generate();
    }

    @Override
    public void processElement(ExecutionRecord sourceExecutionRecord, ProcessFunction<ExecutionRecord, ExecutionRecordResult>.Context ctx, Collector<ExecutionRecordResult> out) throws Exception {

        LOGGER.info("Indexing record: {}", sourceExecutionRecord.getExecutionRecordKey().getRecordId());

        try(Indexer indexer = new IndexerFactory(indexingSettings).getIndexer()) {
            final var properties = new eu.europeana.indexing.IndexingProperties(
                    recordDate, preserveTimestamps, Collections.emptyList(), performRedirect, true);

            LOGGER.info("Indexing: {}", sourceExecutionRecord.getExecutionRecordKey().getRecordId());
            indexer.index(sourceExecutionRecord.getRecordData(), properties, tier -> true);
            LOGGER.info("Indexed: {}", sourceExecutionRecord.getExecutionRecordKey().getRecordId());
            out.collect(ExecutionRecordResult.from(sourceExecutionRecord, taskId, parameterTool.get(JobName.INDEXING)));
        }
    }
}
