package eu.europeana.cloud.flink.client.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobOverviewResponse {
    List<Job> jobs;
    @Getter
    @Setter
    public static class Job {
        String jid;
        @JsonProperty("start-time")
        Long startTime;
    }
}
