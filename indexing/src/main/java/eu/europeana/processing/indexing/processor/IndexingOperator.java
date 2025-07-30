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
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.util.ParameterTool;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.util.Collections;
import java.util.Date;

/**
 * <p>Main operator for {@link eu.europeana.processing.indexing.IndexingJob}.</p>
 * <p>It uses {@link Indexer} to push records to Solr and Mongo</p>
 */
public class IndexingOperator extends ProcessFunction<ExecutionRecord, ExecutionRecordResult> {

    @Serial
    private static final long serialVersionUID = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexingOperator.class);
    private Date recordDate;
    private boolean preserveTimestamps;
    private boolean performRedirect;
    private ParameterTool parameterTool;
    private transient Indexer indexer;
    private long taskId;

    @Override
    public void open(OpenContext openContext) throws Exception {
        parameterTool = ParameterTool.fromMap(getRuntimeContext().getGlobalJobParameters());
        taskId = parameterTool.getLong(JobParamName.TASK_ID);
        IndexingSettings indexingSettings = prepareIndexingSetting(parameterTool);
        recordDate = new Date();
        preserveTimestamps = parameterTool.getBoolean(JobParamName.INDEXING_PRESERVETIMESTAMPS);
        performRedirect = parameterTool.getBoolean(JobParamName.INDEXING_PERFORMREDIRECTS);
        indexer = createIndexerFactory(indexingSettings).getIndexer();
    }

    private IndexingSettings prepareIndexingSetting(ParameterTool parameterTool) throws IndexingException {
       return new IndexingSettingsGenerator(parameterTool).generate();
    }

    protected IndexerFactory createIndexerFactory(IndexingSettings indexingSettings) {
        return new IndexerFactory(indexingSettings);
    }

    @Override
    public void processElement(
        ExecutionRecord sourceExecutionRecord,
        ProcessFunction<ExecutionRecord, ExecutionRecordResult>.Context ctx,
        Collector<ExecutionRecordResult> out) throws IOException {

        LOGGER.info("Indexing record: {}", sourceExecutionRecord.getExecutionRecordKey().getRecordId());

        try {
            indexRecord(sourceExecutionRecord, out);
        } catch (IndexingException e) {
            LOGGER.warn("During indexing record with id: {}, Exception was caught", sourceExecutionRecord.getExecutionRecordKey().getRecordId(), e);
            out.collect(ExecutionRecordResult.from(
                    sourceExecutionRecord,
                    parameterTool.get(JobParamName.TASK_ID),
                    parameterTool.get(JobName.INDEXING),
                    ExecutionRecord.EMPTY,
                    e.getMessage()));
            }
    }

    @Override
    public void close() throws Exception {
        LOGGER.info("Closing indexing operator");
        if (indexer != null){
            indexer.close();
        }
    }

    private void indexRecord(ExecutionRecord sourceExecutionRecord, Collector<ExecutionRecordResult> out) throws IndexingException {
        final var properties = new eu.europeana.indexing.IndexingProperties(
                recordDate, preserveTimestamps, Collections.emptyList(), performRedirect, true);
        LOGGER.info("Indexing: {}", sourceExecutionRecord.getExecutionRecordKey().getRecordId());
        indexer.index(sourceExecutionRecord.getRecordData(), properties, tier -> true);
        LOGGER.info("Indexed: {}", sourceExecutionRecord.getExecutionRecordKey().getRecordId());
        out.collect(ExecutionRecordResult.from(sourceExecutionRecord,
                taskId,
                parameterTool.get(JobName.INDEXING)));
    }
}
