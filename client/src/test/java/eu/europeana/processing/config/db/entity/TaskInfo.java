package eu.europeana.processing.config.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(schema = "batch-framework")
public class TaskInfo {

    @Id
    @Column(nullable = false, length = 50)
    private Long taskId;

    @Column
    private Long commitCount;

    @Column
    private Long writeCount;
}
