package eu.europeana.processing;

import static eu.europeana.processing.job.JobParamName.HTTP_ARCHIVE_URL;


import java.util.Map;
import org.junit.jupiter.api.Test;

public class HttpFlinkPerformanceIT extends OaiFlinkPerformanceIT {

  @Override
  @Test
  void step1_shouldExecuteHarvestCompletelyWithoutErrors() throws Exception {
    executeStep(1, jarIdsProperties.getHttp(), "eu.europeana.processing.http.HttpHarvestingJob",
        Map.of(HTTP_ARCHIVE_URL, sourceProperties.getUrl()));  }
}
