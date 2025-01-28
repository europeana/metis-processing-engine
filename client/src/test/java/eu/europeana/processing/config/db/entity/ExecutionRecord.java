package eu.europeana.processing.config.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "batch-framework", indexes = {@Index(name = "execution_record_dataset_id_execution_id_idx", columnList = "datasetId, executionId")})
public class ExecutionRecord extends ExecutionRecordIdentifier{

  @Column(length = 50)
  private String executionName;
  @Column(columnDefinition = "TEXT")
  private String recordData;
}

