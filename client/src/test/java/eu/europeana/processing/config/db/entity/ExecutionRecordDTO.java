package eu.europeana.processing.config.db.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionRecordDTO {

  private String datasetId;
  private String executionId;
  private String recordId;
  private String executionName;
  private String recordData;
  private String exception;
}
