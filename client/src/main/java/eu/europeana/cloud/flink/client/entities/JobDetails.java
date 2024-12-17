package eu.europeana.cloud.flink.client.entities;

import lombok.Data;

/**
 * Contains information about job. Used by {@link eu.europeana.cloud.flink.client.JobExecutor}
 * while monitoring job progress
 */
@Data
public class JobDetails {

  private String jid;
  private String name;
  private String state;

}
