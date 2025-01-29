package eu.europeana.processing.config.db.repositories;

import eu.europeana.processing.config.db.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.processing.config.db.entity.ExecutionRecordIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRecordExternalIdentifierRepository<T extends ExecutionRecordIdentifier> extends JpaRepository<ExecutionRecordExternalIdentifier, T> {

    Page<ExecutionRecordExternalIdentifier> findAllByExecutionId(String executionId, Pageable pageable);
    long countByDatasetIdAndExecutionId(String datasetId, String executionId);

}
