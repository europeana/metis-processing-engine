package eu.europeana.processing.enrichment.processor;

import eu.europeana.enrichment.rest.client.EnrichmentWorker;
import eu.europeana.enrichment.rest.client.EnrichmentWorkerImpl;
import eu.europeana.enrichment.rest.client.dereference.DereferencerProvider;
import eu.europeana.enrichment.rest.client.enrichment.EnricherProvider;
import eu.europeana.enrichment.rest.client.report.ProcessedResult;
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

import java.util.stream.Collectors;

/**
 * <p>Main operator for {@link eu.europeana.processing.enrichment.EnrichmentJob}.</p>
 * <p>It is responsible for enriching records using {@link EnrichmentWorker}</p>
 */
public class EnrichmentOperator extends ProcessFunction<ExecutionRecord, ExecutionRecordResult> {

    @Serial
    private static final long serialVersionUID = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichmentOperator.class);

    private ParameterTool parameterTool;
    private transient EnrichmentWorker enrichmentWorker;

    @Override
    public void open(OpenContext openContext) throws Exception {
        parameterTool = ParameterTool.fromMap(getRuntimeContext().getGlobalJobParameters());

        String dereferenceURL = parameterTool.getRequired(JobParamName.DEREFERENCE_SERVICE_URL);
        String enrichmentEntityManagementUrl = parameterTool.getRequired(JobParamName.ENRICHMENT_ENTITY_MANAGEMENT_URL);
        String enrichmentEntityApiUrl = parameterTool.getRequired(JobParamName.ENRICHMENT_ENTITY_API_URL);
        String enrichmentEntityApiTokenEndpoint = parameterTool.getRequired(JobParamName.ENRICHMENT_ENTITY_API_TOKEN_ENDPOINT);
        String enrichmentEntityApiGrantParam = parameterTool.getRequired(JobParamName.ENRICHMENT_ENTITY_API_GRANT_PARAMS);

        final EnricherProvider enricherProvider = new EnricherProvider();
        enricherProvider.setEnrichmentPropertiesValues(enrichmentEntityManagementUrl,
                enrichmentEntityApiUrl, enrichmentEntityApiTokenEndpoint, enrichmentEntityApiGrantParam);
        final DereferencerProvider dereferencerProvider = new DereferencerProvider();
        dereferencerProvider.setDereferenceUrl(dereferenceURL);
        dereferencerProvider.setEnrichmentPropertiesValues(enrichmentEntityManagementUrl, enrichmentEntityApiUrl,
                enrichmentEntityApiTokenEndpoint, enrichmentEntityApiGrantParam);

        enrichmentWorker = new EnrichmentWorkerImpl(dereferencerProvider.create(), enricherProvider.create());
        LOGGER.debug("Created enrichment operator.");
    }

    @Override
    public void processElement(
        ExecutionRecord sourceExecutionRecord,
        ProcessFunction<ExecutionRecord, ExecutionRecordResult>.Context ctx,
        Collector<ExecutionRecordResult> out) {
        enrichRecord(sourceExecutionRecord, out);
    }

    private void enrichRecord(ExecutionRecord sourceExecutionRecord, Collector<ExecutionRecordResult> out) {
        ProcessedResult<String> enrichmentResult =
                enrichmentWorker.process(sourceExecutionRecord.getRecordData());
        if (enrichmentResult.getRecordStatus() != ProcessedResult.RecordStatus.CONTINUE) {
            String reportString = enrichmentResult.getReport().stream().map(Object::toString).collect(Collectors.joining("\n"));
            LOGGER.warn("During process of enrichment of record with id: {}, Exceptions: {} were put in report", sourceExecutionRecord.getExecutionRecordKey().getRecordId(), reportString);
            out.collect(
                    ExecutionRecordResult.from(
                            sourceExecutionRecord,
                            parameterTool.get(JobParamName.TASK_ID),
                            JobName.ENRICHMENT,
                            ExecutionRecord.EMPTY,
                            reportString)
            );
        } else {
            out.collect(
                    ExecutionRecordResult.from(
                            sourceExecutionRecord,
                            parameterTool.get(JobParamName.TASK_ID),
                            JobName.ENRICHMENT,
                            enrichmentResult.getProcessedRecord(),
                            null)
            );
        }
    }
}
