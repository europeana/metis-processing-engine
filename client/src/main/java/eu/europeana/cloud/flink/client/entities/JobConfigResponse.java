package eu.europeana.cloud.flink.client.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

import static eu.europeana.cloud.flink.client.entities.JobLocalParams.LOCAL_JOB_ID;

@NoArgsConstructor
@Getter
@Setter
public class JobConfigResponse {
    String jid;
    @JsonProperty("execution-config")
    ExecutionConfig executionConfig;
    @NoArgsConstructor
    @Getter
    @Setter
    public static class ExecutionConfig{
        @JsonProperty("user-config")
        UserConfig userConfig;

        @NoArgsConstructor
        @Getter
        @Setter
        public static class UserConfig{
            @JsonProperty(LOCAL_JOB_ID)
            UUID localJobId;
        }
    }
}
