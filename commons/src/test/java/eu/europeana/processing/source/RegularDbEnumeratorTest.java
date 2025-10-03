package eu.europeana.processing.source;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.model.DataPartition;
import java.util.List;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.util.ParameterTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testcontainers.containers.PostgreSQLContainer;

class RegularDbEnumeratorTest {

  protected static PostgreSQLContainer<?> postgres;

  @Test
  void shouldEmitOneSplit() {

    ParameterTool parameterTool = ParameterTool.fromArgs(
        new String[]{
            "-" + JobParamName.DATASOURCE_URL, postgres.getJdbcUrl(),
            "-" + JobParamName.DATASOURCE_USERNAME, "test",
            "-" + JobParamName.DATASOURCE_PASSWORD, "test",
            "-" + JobParamName.TASK_ID, "1",
            "-" + JobParamName.EXECUTION_ID, "executionId",
            "-" + JobParamName.DATASET_ID, "datasetId"
        }
    );

    SplitEnumeratorContext<DataPartition> mock = Mockito.mock(SplitEnumeratorContext.class);

    RegularDbEnumerator testedEnumerator = new RegularDbEnumerator(mock, parameterTool);
    testedEnumerator.start();
    testedEnumerator.handleSplitRequest(1,"ok");


    ArgumentCaptor<DataPartition> captor = ArgumentCaptor.forClass(DataPartition.class);
    Mockito.verify(mock).assignSplit(captor.capture(), Mockito.eq(1));

    DataPartition capturedDataPartition = captor.getValue();

    Assertions.assertEquals(8, capturedDataPartition.getLimit());
    Assertions.assertEquals(0, capturedDataPartition.getOffset());
    Assertions.assertEquals(0, capturedDataPartition.getProgress());

    testedEnumerator.handleSplitRequest(1,"ok");

    Mockito.verify(mock, Mockito.times(1)).assignSplit(Mockito.any(), Mockito.anyInt());
  }

  @Test
  void shouldEmitTwoSplitsForSmallChunk() {

    ParameterTool parameterTool = ParameterTool.fromArgs(
        new String[]{
            "-" + JobParamName.DATASOURCE_URL, postgres.getJdbcUrl(),
            "-" + JobParamName.DATASOURCE_USERNAME, "test",
            "-" + JobParamName.DATASOURCE_PASSWORD, "test",
            "-" + JobParamName.TASK_ID, "1",
            "-" + JobParamName.EXECUTION_ID, "executionId",
            "-" + JobParamName.DATASET_ID, "datasetId",
            "-" + JobParamName.CHUNK_SIZE, "5"
        }
    );

    SplitEnumeratorContext<DataPartition> mock = Mockito.mock(SplitEnumeratorContext.class);

    RegularDbEnumerator testedEnumerator = new RegularDbEnumerator(mock, parameterTool);
    testedEnumerator.start();
    testedEnumerator.handleSplitRequest(1,"ok");
    testedEnumerator.handleSplitRequest(1,"ok");


    ArgumentCaptor<DataPartition> captor = ArgumentCaptor.forClass(DataPartition.class);
    Mockito.verify(mock, Mockito.times(2)).assignSplit(captor.capture(), Mockito.eq(1));

    List<DataPartition> capturedDataPartitions = captor.getAllValues();

    Assertions.assertEquals(5, capturedDataPartitions.getFirst().getLimit());
    Assertions.assertEquals(0, capturedDataPartitions.getFirst().getOffset());
    Assertions.assertEquals(0, capturedDataPartitions.getFirst().getProgress());

    Assertions.assertEquals(3, capturedDataPartitions.getLast().getLimit());
    Assertions.assertEquals(5, capturedDataPartitions.getLast().getOffset());
    Assertions.assertEquals(0, capturedDataPartitions.getLast().getProgress());

  }

  @BeforeAll
  static void startPostgresDbServer() {

    postgres = new PostgreSQLContainer<>("postgres:15").withDatabaseName("metis-test")
                                                       .withUsername("test").withPassword("test")
                                                       .withInitScripts("metis-processing-schema.sql","testData.sql");
    postgres.start();
  }
}