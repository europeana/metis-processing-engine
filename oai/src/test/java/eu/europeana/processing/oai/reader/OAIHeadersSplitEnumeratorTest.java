package eu.europeana.processing.oai.reader;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.processing.model.DataPartition;
import eu.europeana.processing.model.TaskInfo;
import eu.europeana.processing.oai.repository.OAIHeadersRepository;
import eu.europeana.processing.repository.TaskInfoRepository;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAIHeadersSplitEnumeratorTest extends AbstractOAISourceTest {

  private static final int SUBTASK_ID = 7;
  private String jobUuid= UUID.randomUUID().toString();

  @Mock
  private SplitEnumeratorContext<DataPartition> context;
  @Mock
  private TaskInfo taskInfo;
  private MockedConstruction<OAIBackgroundHeaderHarvester> harvesterConstruction;
  private MockedConstruction<TaskInfoRepository> taskInfoRepositoryConstruction;

  @BeforeEach
  final void setupConstructions() {
    harvesterConstruction = Mockito.mockConstruction(OAIBackgroundHeaderHarvester.class);
    taskInfoRepositoryConstruction = Mockito.mockConstruction(TaskInfoRepository.class, (repository, ctx)
        -> when(repository.findById(anyLong())).thenReturn(Optional.of(taskInfo)));
  }

  @Test
  void shouldStartBackgroundHeadersHarvestingIfNotHarvestedYet() throws IOException {
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.countByDatasetIdAndExecutionId(DATASET, TASK)).thenReturn(0L));
    try (OAIHeadersSplitEnumerator enumerator = new OAIHeadersSplitEnumerator(context, parameterTool, jobUuid)) {

      enumerator.start();

      OAIBackgroundHeaderHarvester harvester = harvesterConstruction.constructed().getFirst();
      verify(harvester).start();
    }
  }

  @Test
  void shouldNotStartBackgroundHarvestingIfHeadersAlreadyHarvested() throws IOException {
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.countByDatasetIdAndExecutionId(DATASET, TASK)).thenReturn(0L));
    OAIEnumeratorState state = OAIEnumeratorState.builder()
                                                 .headersHarvested(true)
                                                 .incompletePartitions(emptyList())
                                                 .build();
    try (OAIHeadersSplitEnumerator enumerator = new OAIHeadersSplitEnumerator(context, parameterTool, jobUuid, state)) {

      enumerator.start();

      OAIBackgroundHeaderHarvester harvester = harvesterConstruction.constructed().getFirst();
      verify(harvester, never()).start();
    }
  }

  @Test
  void shouldBufferSubtaskSplitRequestWhenRecordsAreNotYetAvailableAndAssignSplitWhenNewHeadersSaved() throws IOException {
    mockRunInCoordinatorThreadMethod();
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.countByDatasetIdAndExecutionId(DATASET, TASK)).thenReturn(0L));
    try (OAIHeadersSplitEnumerator enumerator = new OAIHeadersSplitEnumerator(context, parameterTool, jobUuid)) {
      enumerator.start();

      enumerator.handleSplitRequest(SUBTASK_ID, "");
      verify(context, never()).assignSplits(any());
      verify(context, never()).assignSplit(any(), anyInt());
      enumerator.notifyNewHeaderSavedInDB(5);

      verify(context).assignSplit(new DataPartition(0, 5, 0, enumerator.getEnumeratorId()), SUBTASK_ID);
    }
  }

  @Test
  void shouldRememberNumberOfHeaderWhenNewHeadersSavedInDbAndUseItInNextAssignmentRequest() throws IOException {
    mockRunInCoordinatorThreadMethod();
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.countByDatasetIdAndExecutionId(DATASET, TASK)).thenReturn(0L));
    try (OAIHeadersSplitEnumerator enumerator = new OAIHeadersSplitEnumerator(context, parameterTool, jobUuid)) {
      enumerator.start();

      enumerator.notifyNewHeaderSavedInDB(5);
      enumerator.handleSplitRequest(SUBTASK_ID, "");

      verify(context).assignSplit(new DataPartition(0, 5, 0, enumerator.getEnumeratorId()), SUBTASK_ID);
    }
  }

  @Test
  void shouldSaveInformationAboutHeaderHarvestingFinishedInSnapshot() throws IOException {
    mockRunInCoordinatorThreadMethod();
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.countByDatasetIdAndExecutionId(DATASET, TASK)).thenReturn(0L));
    try (OAIHeadersSplitEnumerator enumerator = new OAIHeadersSplitEnumerator(context, parameterTool, jobUuid)) {
      enumerator.start();
      enumerator.notifyHeaderHarvestingFinished();

      OAIEnumeratorState snapshot = enumerator.snapshotState(0);

      assertTrue(snapshot.isHeadersHarvested());
    }
  }

  @Test
  void shouldEndAfterHeadersHarvestingFinishedAndThereAreNoMoreRecordsToSave() throws IOException {
    mockRunInCoordinatorThreadMethod();
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.countByDatasetIdAndExecutionId(DATASET, TASK)).thenReturn(0L));
    try (OAIHeadersSplitEnumerator enumerator = new OAIHeadersSplitEnumerator(context, parameterTool, jobUuid)) {
      enumerator.start();

      enumerator.notifyHeaderHarvestingFinished();
      enumerator.handleSplitRequest(SUBTASK_ID, "");

      verify(context).signalNoMoreSplits(SUBTASK_ID);
    }
  }

  @Test
  void shouldEndBufferedReadersIfHeadersHarvestingFinishedAndThereAreNoMoreRecordsToSave() throws IOException {
    mockRunInCoordinatorThreadMethod();
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.countByDatasetIdAndExecutionId(DATASET, TASK)).thenReturn(0L));
    try (OAIHeadersSplitEnumerator enumerator = new OAIHeadersSplitEnumerator(context, parameterTool, jobUuid)) {
      enumerator.start();
      enumerator.handleSplitRequest(SUBTASK_ID, "");

      enumerator.notifyHeaderHarvestingFinished();

      verify(context).signalNoMoreSplits(SUBTASK_ID);
    }
  }

  @Test
  void shouldThrowExceptionIfBackgroundHeadersHarvestingFailed() throws IOException {
    Exception backgroundException = new Exception("Could not harvest!");
    mockRunInCoordinatorThreadMethod();
    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.countByDatasetIdAndExecutionId(DATASET, TASK)).thenReturn(0L));
    try (OAIHeadersSplitEnumerator enumerator = new OAIHeadersSplitEnumerator(context, parameterTool, jobUuid)) {
      enumerator.start();

      assertThrows(RuntimeException.class,
          ()->enumerator.notifyHeadersHarvestingFailed(backgroundException));
    }
  }

  @AfterEach
  final void cleanupTheseConstructions() {
    harvesterConstruction.close();
    taskInfoRepositoryConstruction.close();
  }

  private void mockRunInCoordinatorThreadMethod() {
    doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    }).when(context).runInCoordinatorThread(any());
  }
}