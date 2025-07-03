package eu.europeana.processing.indexing;

import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOAPPLICATIONNAME;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOAUTHDB;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGODBNAME;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOINSTANCES;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOPASSWORD;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOPOOLSIZE;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOPORTNUMBER;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOREADPREFERENCE;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOREDIRECTDBNAME;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOUSERNAME;
import static eu.europeana.processing.job.JobParamName.INDEXING_MONGOUSESSL;
import static eu.europeana.processing.job.JobParamName.INDEXING_PERFORMREDIRECTS;
import static eu.europeana.processing.job.JobParamName.INDEXING_PRESERVETIMESTAMPS;
import static eu.europeana.processing.job.JobParamName.INDEXING_SOLRINSTANCES;
import static eu.europeana.processing.job.JobParamName.INDEXING_ZOOKEEPERCHROOT;
import static eu.europeana.processing.job.JobParamName.INDEXING_ZOOKEEPERDEFAULTCOLLECTION;
import static eu.europeana.processing.job.JobParamName.INDEXING_ZOOKEEPERINSTANCES;
import static eu.europeana.processing.job.JobParamName.INDEXING_ZOOKEEPERPORTNUMBER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europeana.indexing.Indexer;
import eu.europeana.indexing.IndexerFactory;
import eu.europeana.indexing.IndexingProperties;
import eu.europeana.indexing.IndexingSettings;
import eu.europeana.indexing.exception.IndexingException;
import eu.europeana.indexing.exception.RecordRelatedIndexingException;
import eu.europeana.processing.indexing.processor.IndexingOperator;
import eu.europeana.processing.model.ExecutionRecord;
import eu.europeana.processing.model.ExecutionRecordResult;
import eu.europeana.processing.test.common.AbstractJobTest;
import java.util.function.Predicate;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndexingJobTest extends AbstractJobTest {

  private static class IndexingJobWithMockedIndexerFactory extends IndexingJob {

    private final boolean indexingSuccessful;

    public IndexingJobWithMockedIndexerFactory(String[] args, boolean indexingSuccessful) {
      super(args);
      this.indexingSuccessful = indexingSuccessful;
    }

    @Override
    public ProcessFunction<ExecutionRecord, ExecutionRecordResult> getMainOperator() {
      boolean successful = indexingSuccessful;
      return new IndexingOperator() {
        @Override
        protected IndexerFactory createIndexerFactory(IndexingSettings indexingSettings) {
          try {
            IndexerFactory indexerFactory = mock(IndexerFactory.class);
            Indexer indexer = mock(Indexer.class);
            if (!successful) {
              doThrow(new RecordRelatedIndexingException("TestingIndexingError")).when(indexer)
                                                                                 .index(anyString(),
                                                                                     any(IndexingProperties.class),
                                                                                     any(Predicate.class));
            }
            when(indexerFactory.getIndexer()).thenReturn(indexer);
            return indexerFactory;
          } catch (IndexingException e) {
            throw new RuntimeException(e);
          }
        }
      };
    }
  }

  @Test
  void shouldProperlyRunIndexing() throws Exception {
    startPostgresDbServer("indexing-input.sql");
    String[] args = prepareIndxingJobArgs();

    new IndexingJobWithMockedIndexerFactory(args, true).execute();

    assertThatResultRowIsSavedInDb();
    assertNoErrorsSavedInDb();
  }


  @Test
  void shouldSaveRecordErrorInDB() throws Exception {
    startPostgresDbServer("indexing-input.sql");
    String[] args = prepareIndxingJobArgs();

    new IndexingJobWithMockedIndexerFactory(args, false).execute();

    assertThatErrorIsSavedInDb();
  }

  private String[] prepareIndxingJobArgs() {
    return prepareArgs("--" + INDEXING_PRESERVETIMESTAMPS, "false",
        "--" + INDEXING_PERFORMREDIRECTS, "true",
        "--" + INDEXING_MONGOINSTANCES, "mongo1.example.org,mongo2.example.org,mongo3.example.org",
        "--" + INDEXING_MONGOPORTNUMBER, "10010",
        "--" + INDEXING_MONGODBNAME, "preview-test",
        "--" + INDEXING_MONGOREDIRECTDBNAME, "redirect-preview-test",
        "--" + INDEXING_MONGOUSERNAME, "test",
        "--" + INDEXING_MONGOPASSWORD, "*****",
        "--" + INDEXING_MONGOAUTHDB, "admin",
        "--" + INDEXING_MONGOUSESSL, "false",
        "--" + INDEXING_MONGOREADPREFERENCE, "PRIMARY_PREFERRED",
        "--" + INDEXING_MONGOPOOLSIZE, "4",
        "--" + INDEXING_SOLRINSTANCES, "solr1.example.org:10020/search_test_preview,solr2.example.org:10020/search_test_preview,solr3.example.org:10020/search_test_preview",
        "--" + INDEXING_ZOOKEEPERINSTANCES, "zoo-1.example.org",
        "--" + INDEXING_ZOOKEEPERPORTNUMBER, "2281",
        "--" + INDEXING_ZOOKEEPERCHROOT, "/",
        "--" + INDEXING_ZOOKEEPERDEFAULTCOLLECTION, "search_test_preview",
        "--" + INDEXING_MONGOAPPLICATIONNAME, "batch-indexing");
  }

  @Override
  protected int stepNumber() {
    return 8;
  }

}