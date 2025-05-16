package eu.europeana.processing.sink;


import eu.europeana.processing.DbConnectionProvider;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.repository.ExecutionRecordExceptionLogRepository;
import eu.europeana.processing.repository.ExecutionRecordRepository;
import eu.europeana.processing.retryable.RetryableMethodExecutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;
import org.apache.flink.util.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;

public class DbSink implements Sink<ExecutionRecordResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DbSink.class);
    private final ParameterTool tool;
    public DbSink(ParameterTool tool) {
        this.tool = tool;
        LOGGER.debug("DbSink initialized");
    }

    @Override
    public SinkWriter<ExecutionRecordResult> createWriter(WriterInitContext context) {
        return new DbSinkWriter(tool);
    }

    public static class DbSinkWriter implements SinkWriter<ExecutionRecordResult>, Serializable {
        private static final Logger LOGGER = LoggerFactory.getLogger(DbSinkWriter.class);

        private final transient ExecutionRecordRepository executionRecordRepository;
        private final transient ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;
        private final transient DbConnectionProvider dbConnectionProvider;

        public DbSinkWriter(ParameterTool tool) {
            dbConnectionProvider = new DbConnectionProvider(tool);
            executionRecordRepository = RetryableMethodExecutor.createRetryProxy(new ExecutionRecordRepository(dbConnectionProvider));
            executionRecordExceptionLogRepository =
                    RetryableMethodExecutor.createRetryProxy(new ExecutionRecordExceptionLogRepository(dbConnectionProvider));
            LOGGER.debug("DbSinkWriter initialized");
        }

        @Override
        public void write(ExecutionRecordResult executionRecordResult, Context context) throws IOException, InterruptedException {
            if (Thread.interrupted()) {
                LOGGER.warn("Thread interruption detected when processing element {}", executionRecordResult.getExecutionRecord().getExecutionRecordKey().getRecordId());
                throw new InterruptedException();
            }

            if (recordProcessedSuccessfully(executionRecordResult)) {
                storeProcessedRecord(executionRecordResult);
            } else {
                storeExecutionRecordException(executionRecordResult);
            }

            LOGGER.info("Written element {}", executionRecordResult.getExecutionRecord().getExecutionRecordKey().getRecordId());
        }

        private boolean recordProcessedSuccessfully(ExecutionRecordResult executionRecordResult) {
            return StringUtils.isEmpty(executionRecordResult.getException());
        }

        private void storeProcessedRecord(ExecutionRecordResult executionRecordResult) throws IOException {
            executionRecordRepository.save(executionRecordResult);
        }

        private void storeExecutionRecordException(ExecutionRecordResult executionRecordResult) {
            executionRecordExceptionLogRepository.save(executionRecordResult);
        }

        // TODO: Consider adding buffer for records and then batch add to DB
        // It would require for our repositories to support batch operations and to implement some sort of cache in sink
        // We would gain efficiency but drawback would be slightly increased memory usage and higher delay in results delivery
        // Additionally we can consider adding committer on top of things described above in order to guarantee exactly once semantic
        @Override
        public void flush(boolean endOfInput) {
            LOGGER.debug("DbSinkWriter flushed!");
        }

        @Override
        public void close() throws Exception {
            if (dbConnectionProvider != null) {
                dbConnectionProvider.close();
            }
            LOGGER.debug("DbSinkWriter closed!");
        }
    }
}
