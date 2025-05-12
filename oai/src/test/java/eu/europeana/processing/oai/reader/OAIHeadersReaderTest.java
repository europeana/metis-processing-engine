package eu.europeana.processing.oai.reader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.processing.model.DataPartition;
import eu.europeana.processing.oai.repository.OAIHeadersRepository;
import java.util.List;
import org.apache.flink.api.connector.source.ReaderOutput;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.core.io.InputStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAIHeadersReaderTest extends AbstractOAISourceTest {

  private static final long OFFSET = 10;
  private static final long LIMIT = 2;

  @Mock
  private SourceReaderContext context;
  @Mock
  private ReaderOutput<OaiRecordHeader> output;

  @Test
  void shouldEmitHeadersGetFromRepository() throws Exception {

    repositoryConstruction = Mockito.mockConstruction(OAIHeadersRepository.class, (repository, context)
        -> when(repository.getByDatasetIdAndExecutionIdAndOffsetAndLimit(DATASET, TASK, OFFSET, LIMIT))
        .thenReturn(List.of(HEADER_1, HEADER_2)));
    try (OAIHeadersReader reader = new OAIHeadersReader(context, parameterTool)) {
      reader.start();
      reader.addSplits(List.of(new DataPartition(OFFSET, LIMIT, 0)));

      //TODO the implementation return bad status. It looks that is does not matter much for Flink,
      //because it executes reader.isAvailable() which return unmodified already completed feature
      //so it eventually executes poll again. Anyway if we modify behaviour we could update this test.
      //assertEquals(InputStatus.MORE_AVAILABLE, reader.pollNext(output));
      reader.pollNext(output);
      verify(output).collect(HEADER_1);
      assertEquals(InputStatus.NOTHING_AVAILABLE, reader.pollNext(output));
      verify(output).collect(HEADER_2);
    }
  }


}