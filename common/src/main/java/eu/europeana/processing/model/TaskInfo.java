package eu.europeana.processing.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Class describing current task
 */
@Entity
@Table(schema = "batch-framework")
@Getter
@Setter
@Builder
@AllArgsConstructor
@ToString
public class TaskInfo {

  private @Id long taskId;

  private String taskName;
  @Transient
  private Map<String, String> parameters;

  @Temporal(TemporalType.TIMESTAMP)
  private Date startTime;

  @Temporal(TemporalType.TIMESTAMP)
  private Date endTime;

  private long commitCount;
  private long writeCount;

  public TaskInfo() {

  }

}
