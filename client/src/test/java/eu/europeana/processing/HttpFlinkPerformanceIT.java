package eu.europeana.processing;

import static eu.europeana.processing.job.JobParamName.HTTP_ARCHIVE_URL;


import java.util.Map;
import org.junit.jupiter.api.Test;

public class HttpFlinkPerformanceIT extends FlinkPerformanceIT {

  @Override
  @Test
  void step1_shouldExecuteOAIHarvestComplietellyWithoutErrors() throws Exception {
    executeStep(1, jarIdsProperties.getHttp(), "eu.europeana.processing.http.HttpJob",
        Map.of(HTTP_ARCHIVE_URL, sourceProperties.getUrl()));  }
}
