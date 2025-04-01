package eu.europeana.processing.oai.reader;

import static eu.europeana.processing.job.JobParamName.DATASET_ID;
import static eu.europeana.processing.job.JobParamName.METADATA_PREFIX;
import static eu.europeana.processing.job.JobParamName.OAI_REPOSITORY_URL;
import static eu.europeana.processing.job.JobParamName.SET_SPEC;
import static eu.europeana.processing.job.JobParamName.TASK_ID;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.processing.oai.repository.OAIHeadersRepository;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.apache.flink.api.java.utils.ParameterTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAIBackgroundHeaderHarvesterTest extends AbstractOAISourceTest {

  @Mock
  private OAIHeadersSplitEnumerator enumerator;

  private OAIBackgroundHeaderHarvester harvester;

  @Test
  void shouldHarvestAndSaveHeadersInDbWithAndNotifyEnumerator() throws IOException {
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.save(any(), any(), any(), anyInt())).thenReturn(true));
    ParameterTool parameterTool = ParameterTool.fromMap(Map.of(DATASET_ID, DATASET, TASK_ID, TASK,
        OAI_REPOSITORY_URL, "https://metis-repository-rest.test.eanadev.org/repository/oai",
        METADATA_PREFIX, "edm",
        SET_SPEC, "ecloud_e2e_tests_without_4_records"));
    harvester = new OAIBackgroundHeaderHarvester(enumerator, parameterTool);

    harvester.start();
    await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200))
           .untilAsserted(() -> verify(enumerator).notifyHeaderHarvestingFinished());

    OAIHeadersRepository repository = repositoryConstruction.constructed().getFirst();
    InOrder order = inOrder(repository, enumerator);
    order.verify(repository).save(eq(DATASET), eq(TASK), refEq(HEADER_1), eq(0));
    order.verify(enumerator).notifyNewHeaderSavedInDB(1);
    order.verify(repository).save(eq(DATASET), eq(TASK), refEq(HEADER_2), eq(1));
    order.verify(enumerator).notifyNewHeaderSavedInDB(2);
    order.verify(repository).save(eq(DATASET), eq(TASK), refEq(HEADER_3), eq(2));
    order.verify(enumerator).notifyNewHeaderSavedInDB(3);
    order.verify(repository).save(eq(DATASET), eq(TASK), refEq(HEADER_4), eq(3));
    order.verify(enumerator).notifyNewHeaderSavedInDB(4);
    order.verify(enumerator).notifyHeaderHarvestingFinished();
  }

  @Test
  void shouldNotCountRecordsAlreadyPresentInDbDuringHarvesting() throws IOException {
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> {
      when(repository.save(any(), any(), any(), anyInt())).thenReturn(false, true, true, false);
      when(repository.countByDatasetIdAndExecutionId(any(), any())).thenReturn(2L);
    });
    ParameterTool parameterTool = ParameterTool.fromMap(Map.of(DATASET_ID, DATASET, TASK_ID, TASK,
        OAI_REPOSITORY_URL, "https://metis-repository-rest.test.eanadev.org/repository/oai",
        METADATA_PREFIX, "edm",
        SET_SPEC, "ecloud_e2e_tests_without_4_records"));
    harvester = new OAIBackgroundHeaderHarvester(enumerator, parameterTool);

    harvester.start();
    await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200))
           .untilAsserted(() -> verify(enumerator).notifyHeaderHarvestingFinished());

    OAIHeadersRepository repository = repositoryConstruction.constructed().getFirst();
    InOrder order = inOrder(repository, enumerator);
    order.verify(enumerator).notifyNewHeaderSavedInDB(2);
    order.verify(repository).save(eq(DATASET), eq(TASK), refEq(HEADER_1), eq(2));
    order.verify(repository).save(eq(DATASET), eq(TASK), refEq(HEADER_2), eq(2));
    order.verify(enumerator).notifyNewHeaderSavedInDB(3);
    order.verify(repository).save(eq(DATASET), eq(TASK), refEq(HEADER_3), eq(3));
    order.verify(enumerator).notifyNewHeaderSavedInDB(4);
    order.verify(repository).save(eq(DATASET), eq(TASK), refEq(HEADER_4), eq(4));
    order.verify(enumerator).notifyHeaderHarvestingFinished();
    verifyNoMoreInteractions(enumerator);
  }

  @Test
  void shouldNotifyEnumeratorAboutFailure() {
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.save(any(), any(), any(), anyInt())).thenReturn(true));
    ParameterTool parameterTool = ParameterTool.fromMap(Map.of(DATASET_ID, DATASET, TASK_ID, TASK,
        OAI_REPOSITORY_URL, "https://unknown-dns-adres14395.eanadev.org/repository/oai",
        METADATA_PREFIX, "edm",
        SET_SPEC, "ecloud_e2e_tests_without_4_records"));
    harvester = new OAIBackgroundHeaderHarvester(enumerator, parameterTool);

    harvester.start();
    await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200))
           .untilAsserted(() -> verify(enumerator).notifyHeadersHarvestingFailed(any()));

    verify(enumerator).notifyHeadersHarvestingFailed(any(HarvesterException.class));
  }

  @Test
  void shouldStopProcessingWhenCloseInvoked() throws IOException, InterruptedException {
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.save(any(), any(), any(), anyInt())).thenReturn(true));
    ParameterTool parameterTool = ParameterTool.fromMap(Map.of(DATASET_ID, DATASET, TASK_ID, TASK,
        OAI_REPOSITORY_URL, "https://metis-repository-rest.test.eanadev.org/repository/oai",
        METADATA_PREFIX, "edm",
        SET_SPEC, "ecloud_e2e_tests_without_4_records"));
    harvester = new OAIBackgroundHeaderHarvester(enumerator, parameterTool);

    harvester.start();
    OAIHeadersRepository repository = repositoryConstruction.constructed().getFirst();
    harvester.close();
    verify(repository, never()).save(any(), any(), any(), anyInt());
    verifyNoInteractions(enumerator);
  }

}