package eu.europeana.cloud.flink.client.entities;


import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Value;

import static eu.europeana.cloud.flink.client.entities.JobLocalParams.LOCAL_JOB_ID;

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
  @JsonIgnore
  UUID localJobId;

  public static class SubmitJobRequestBuilder {

    public SubmitJobRequestBuilder programArgs(Map<String, Object> argsMap) {
      this.localJobId = UUID.randomUUID();
      this.programArgs = argsMap.entrySet().stream()
                                .map(entry -> "--" + entry.getKey() + " " + entry.getValue())
                                .collect(Collectors.joining(" "))
              .concat(" --"+LOCAL_JOB_ID+" "+ localJobId);
      return this;
    }
  }
}
