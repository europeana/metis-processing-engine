package eu.europeana.processing.config.db.repositories;

import eu.europeana.processing.model.TaskInfo;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskInfoRepository extends JpaRepository<TaskInfo, Long> {
    TaskInfo getByTaskId(Long taskId);
    List<TaskInfo> findByTaskName(String taskName, Limit limit);
}
