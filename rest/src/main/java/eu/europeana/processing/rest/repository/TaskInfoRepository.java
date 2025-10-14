package eu.europeana.processing.rest.repository;

import eu.europeana.processing.model.TaskInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskInfoRepository extends JpaRepository<TaskInfo, String> {

}
