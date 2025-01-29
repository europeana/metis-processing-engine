package eu.europeana.cloud.flink.client.entities;


import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;

/**
 * Describes arguments used while submitting job to the JobManager
 */
@Value
@Builder
public class SubmitJobRequest {

  String entryClass;
  String parallelism;
  String programArgs;
  String savepointPath;
  boolean allowNonRestoredState;

  public static class SubmitJobRequestBuilder {

    public SubmitJobRequestBuilder programArgs(Map<String, Object> argsMap) {
      this.programArgs = argsMap.entrySet().stream()
                                .map(entry -> "--" + entry.getKey() + " " + entry.getValue())
                                .collect(Collectors.joining(" "));
      return this;
    }
  }
}
