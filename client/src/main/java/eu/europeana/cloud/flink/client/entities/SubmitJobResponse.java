package eu.europeana.cloud.flink.client.entities;

import lombok.Data;

@Data
public class SubmitJobResponse {

  //Do not change field name because Flink API uses this direct name and it would break client code.
  private String jobid;

}
