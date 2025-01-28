package eu.europeana.processing.config.db.repositories;

import eu.europeana.processing.config.db.entity.ExecutionRecordExceptionLog;
import eu.europeana.processing.config.db.entity.ExecutionRecordIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordExceptionLogRepository<T extends ExecutionRecordIdentifier> extends JpaRepository<ExecutionRecordExceptionLog, T> {
  long countByDatasetIdAndExecutionId(String datasetId, String executionId);
}
