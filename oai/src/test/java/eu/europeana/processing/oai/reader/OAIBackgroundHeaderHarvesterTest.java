package eu.europeana.processing.oai.reader;

import static eu.europeana.processing.job.JobParamName.DATASET_ID;
import static eu.europeana.processing.job.JobParamName.METADATA_PREFIX;
import static eu.europeana.processing.job.JobParamName.OAI_REPOSITORY_URL;
import static eu.europeana.processing.job.JobParamName.SET_SPEC;
import static eu.europeana.processing.job.JobParamName.TASK_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.processing.oai.repository.BatchHeaderSaver;
import eu.europeana.processing.oai.repository.OAIHeadersRepository;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.flink.util.ParameterTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAIBackgroundHeaderHarvesterTest extends AbstractOAISourceTest {

  private String jobUuid=UUID.randomUUID().toString();
  @Mock
  private OAIHeadersSplitEnumerator enumerator;
  @Captor
  private ArgumentCaptor<List<OaiRecordHeader>> headersCaptor;
  @Captor
  private ArgumentCaptor<Integer> savedCountCaptor;
  @Captor
  private ArgumentCaptor<Integer> notifiedCountCaptor;


  private OAIBackgroundHeaderHarvester harvester;


  @Test
  void shouldHarvestAndSaveHeadersInDbWithAndNotifyEnumerator() throws IOException {
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.getExistingIdentifiers(any(), any(), any())).thenReturn(Collections.emptySet()));
    ParameterTool parameterTool = ParameterTool.fromMap(Map.of(DATASET_ID, DATASET, TASK_ID, TASK,
        OAI_REPOSITORY_URL, "https://metis-repository-rest.test.eanadev.org/repository/oai",
        METADATA_PREFIX, "edm",
        SET_SPEC, "ecloud_e2e_tests_without_4_records"));
    harvester = new OAIBackgroundHeaderHarvester(enumerator, parameterTool, jobUuid);

    harvester.start();
    await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200))
           .untilAsserted(() -> verify(enumerator).notifyHeaderHarvestingFinished());

    OAIHeadersRepository repository = repositoryConstruction.constructed().getFirst();
    InOrder order = inOrder(repository, enumerator);
    order.verify(repository).save(eq(DATASET), eq(TASK), headersCaptor.capture(), eq(0));
    assertThat(headersCaptor.getValue()).usingRecursiveComparison().isEqualTo(List.of(HEADER_1, HEADER_2, HEADER_3, HEADER_4));
    order.verify(enumerator).notifyNewHeaderSavedInDB(4);
    order.verify(enumerator).notifyHeaderHarvestingFinished();
  }

  @Test
  void shouldNotCountRecordsAlreadyPresentInDbDuringHarvesting() throws IOException {
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> {
      when(repository.getExistingIdentifiers(any(), any(), any())).thenReturn(Set.of(RECORD_ID_1, RECORD_ID_4));
      when(repository.countByDatasetIdAndExecutionId(any(), any())).thenReturn(2L);
    });
    ParameterTool parameterTool = ParameterTool.fromMap(Map.of(DATASET_ID, DATASET, TASK_ID, TASK,
        OAI_REPOSITORY_URL, "https://metis-repository-rest.test.eanadev.org/repository/oai",
        METADATA_PREFIX, "edm",
        SET_SPEC, "ecloud_e2e_tests_without_4_records"));
    harvester = new OAIBackgroundHeaderHarvester(enumerator, parameterTool, jobUuid);

    harvester.start();
    await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200))
           .untilAsserted(() -> verify(enumerator).notifyHeaderHarvestingFinished());

    OAIHeadersRepository repository = repositoryConstruction.constructed().getFirst();
    InOrder order = inOrder(repository, enumerator);
    order.verify(enumerator).notifyNewHeaderSavedInDB(2);
    order.verify(repository).save(eq(DATASET), eq(TASK), headersCaptor.capture(), eq(2));
    assertThat(headersCaptor.getValue()).usingRecursiveComparison().isEqualTo(List.of(HEADER_2, HEADER_3));
    order.verify(enumerator).notifyNewHeaderSavedInDB(4);
    order.verify(enumerator).notifyHeaderHarvestingFinished();
    verifyNoMoreInteractions(enumerator);
  }

  @Test
  void shouldNotifyEnumeratorAboutFailure() {
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.getExistingIdentifiers(any(), any(), any())).thenReturn(Collections.emptySet()));
    ParameterTool parameterTool = ParameterTool.fromMap(Map.of(DATASET_ID, DATASET, TASK_ID, TASK,
        OAI_REPOSITORY_URL, "https://unknown-dns-adres14395.eanadev.org/repository/oai",
        METADATA_PREFIX, "edm",
        SET_SPEC, "ecloud_e2e_tests_without_4_records"));
    harvester = new OAIBackgroundHeaderHarvester(enumerator, parameterTool, jobUuid);

    harvester.start();
    await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200))
           .untilAsserted(() -> verify(enumerator).notifyHeadersHarvestingFailed(any()));

    verify(enumerator).notifyHeadersHarvestingFailed(any(HarvesterException.class));
  }

  @Test
  void shouldStopProcessingWhenCloseInvoked() throws IOException, InterruptedException {
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.getExistingIdentifiers(any(), any(), any())).thenReturn(Collections.emptySet()));
    ParameterTool parameterTool = ParameterTool.fromMap(Map.of(DATASET_ID, DATASET, TASK_ID, TASK,
        OAI_REPOSITORY_URL, "https://metis-repository-rest.test.eanadev.org/repository/oai",
        METADATA_PREFIX, "edm",
        SET_SPEC, "ecloud_e2e_tests_without_4_records"));
    harvester = new OAIBackgroundHeaderHarvester(enumerator, parameterTool, jobUuid);
    harvester.start();
    OAIHeadersRepository repository = repositoryConstruction.constructed().getFirst();
    //We sleep to ensure that background executor had time to start.
    Thread.sleep(500);
    harvester.close();
    verify(repository, never()).save(any(), any(), any(), anyInt());
    verifyNoInteractions(enumerator);
  }

  @Test
  void shouldHarvestBiggerSetAndNotifyEnumerator() throws IOException {
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.getExistingIdentifiers(any(), any(), any())).thenReturn(Collections.emptySet()));
    ParameterTool parameterTool = ParameterTool.fromMap(Map.of(DATASET_ID, DATASET, TASK_ID, TASK,
        OAI_REPOSITORY_URL, "https://metis-repository-rest.test.eanadev.org/repository/oai",
        METADATA_PREFIX, "edm",
        SET_SPEC, "Heide1000records"));
    harvester = new OAIBackgroundHeaderHarvester(enumerator, parameterTool, jobUuid);

    harvester.start();
    await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200))
           .untilAsserted(() -> verify(enumerator).notifyHeaderHarvestingFinished());

    OAIHeadersRepository repository = repositoryConstruction.constructed().getFirst();
    verify(repository, atLeastOnce()).save(eq(DATASET), eq(TASK), headersCaptor.capture(), savedCountCaptor.capture());
    verify(enumerator, atLeastOnce()).notifyNewHeaderSavedInDB(notifiedCountCaptor.capture());
    verify(enumerator).notifyHeaderHarvestingFinished();
    assertEquals(1000, headersCaptor.getAllValues().stream().mapToInt(List::size).sum());
    //Index of first element in last batch could not be smaller than: 1000 - batch size
    assertThat(savedCountCaptor.getValue()).isGreaterThanOrEqualTo(1000 - BatchHeaderSaver.MAX_BATCH_SIZE);
    assertEquals(1000, notifiedCountCaptor.getValue());
  }

}