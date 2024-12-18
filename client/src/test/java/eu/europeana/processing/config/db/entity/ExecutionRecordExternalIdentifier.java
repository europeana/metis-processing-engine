package eu.europeana.processing.config.db.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "batch-framework")
public class ExecutionRecordExternalIdentifier extends ExecutionRecordIdentifier{

    private boolean isDeleted;
}
