package eu.europeana.processing.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
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
@Getter
@Setter
@Builder
@AllArgsConstructor
@ToString
public class TaskInfo {

  private @Id long taskId;
  /**
   * This it transient by purpose and temporarily. I wanted to have task name in task info, but
   * I didn't want to change DB schemat because it would require multiple changes in jobs.
   * It is not needed for this PoC
   */
  @Transient
  private String taskName;
  @Transient
  private Map<String, String> parameters;

  private long commitCount;
  private long writeCount;

  public TaskInfo() {

  }

}
