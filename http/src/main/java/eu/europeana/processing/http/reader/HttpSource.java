package eu.europeana.processing.http.reader;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.source.ObjectStreamVersionedSerializer;
import java.io.Serial;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.io.SimpleVersionedSerializer;

/**
 * Http source. It reads archive file, like zip, tar from the http and emits (xml) files contained in it. Every file is emitted as
 * ExecutionRecordResult which contains file content as String and path to the file for debug reasons. The path is relative if the
 * file is extracted on fly, or full if the file is initially extracted in the directory on the cluster.
 */
public class HttpSource implements Source<ExecutionRecordResult, HttpSourceSplit, HttpEnumeratorState> {
  @Serial
  private static final long serialVersionUID = 1;

  private static final String METIS_PROCESSING_HTTP_JOBS_DIR_ENV_NAME = "METIS_PROCESSING_HTTP_JOBS_DIR";
  private static final String HTTP_JOBS_DIR =
      Optional.ofNullable(System.getenv(METIS_PROCESSING_HTTP_JOBS_DIR_ENV_NAME)).orElse("/http-jobs");

  private final ParameterTool parameterTool;
  private final String jobDirectoryPath;


  /**
   * Creates HttpSource
   * @param parameterTool - all the command line parameters of the job
   */
  public HttpSource(ParameterTool parameterTool) {
    this.parameterTool = parameterTool;
    long taskId = parameterTool.getLong(JobParamName.TASK_ID);
    this.jobDirectoryPath = evaluateHttpJobFolderPath(taskId);
  }

  @Override
  public Boundedness getBoundedness() {
    return Boundedness.BOUNDED;
  }

  @Override
  public SplitEnumerator<HttpSourceSplit, HttpEnumeratorState> createEnumerator(
      SplitEnumeratorContext<HttpSourceSplit> enumContext) {
    return new HttpEnumerator(enumContext, null, parameterTool, jobDirectoryPath);
  }

  @Override
  public SplitEnumerator<HttpSourceSplit, HttpEnumeratorState> restoreEnumerator(
      SplitEnumeratorContext<HttpSourceSplit> enumContext, HttpEnumeratorState state) {
    return new HttpEnumerator(enumContext, state, parameterTool, jobDirectoryPath);
  }

  @Override
  public SourceReader<ExecutionRecordResult, HttpSourceSplit> createReader(SourceReaderContext readerContext) {
    return new HttpReader(readerContext, parameterTool);
  }

  @Override
  public SimpleVersionedSerializer<HttpSourceSplit> getSplitSerializer() {
    return new ObjectStreamVersionedSerializer<>();
  }

  @Override
  public SimpleVersionedSerializer<HttpEnumeratorState> getEnumeratorCheckpointSerializer() {
    return new ObjectStreamVersionedSerializer<>();
  }

  private static String evaluateHttpJobFolderPath(long taskId) {
    return Path.of(HTTP_JOBS_DIR).resolve("task_" + taskId + "_" + UUID.randomUUID()).toString();
  }
}
