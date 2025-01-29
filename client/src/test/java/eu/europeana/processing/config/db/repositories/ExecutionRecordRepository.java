package eu.europeana.processing.config.db.repositories;

import eu.europeana.processing.config.db.entity.ExecutionRecord;
import eu.europeana.processing.config.db.entity.ExecutionRecordIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRecordRepository<T extends ExecutionRecordIdentifier> extends JpaRepository<ExecutionRecord, T> {
  Page<ExecutionRecord> findByDatasetIdAndExecutionId(String datasetId, String executionId, Pageable pageable);
  long countByDatasetIdAndExecutionId(String datasetId, String executionId);
}
