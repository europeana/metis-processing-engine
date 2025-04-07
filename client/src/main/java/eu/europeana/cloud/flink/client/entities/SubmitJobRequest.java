package eu.europeana.cloud.flink.client.entities;


import java.util.ArrayList;
import java.util.List;
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
  ArrayList<String> programArgsList;
  String savepointPath;
  boolean allowNonRestoredState;
  @JsonIgnore
  UUID localJobId;

  public static class SubmitJobRequestBuilder {

    public SubmitJobRequestBuilder programArgs(Map<String, Object> argsMap) {
      this.localJobId = UUID.randomUUID();
      this.programArgsList = new ArrayList<>();
      argsMap.forEach((key, value) -> {
        programArgsList.add("--" + key);
        programArgsList.add(String.valueOf(value));
      });

      programArgsList.add("--" + LOCAL_JOB_ID);
      programArgsList.add(String.valueOf(localJobId));
      return this;
    }
  }
}
