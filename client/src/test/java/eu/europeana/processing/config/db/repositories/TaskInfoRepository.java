package eu.europeana.processing.config.db.repositories;

import eu.europeana.processing.config.db.entity.TaskInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskInfoRepository extends JpaRepository<TaskInfo, Long> {
    TaskInfo getByTaskId(Long taskId);
}
