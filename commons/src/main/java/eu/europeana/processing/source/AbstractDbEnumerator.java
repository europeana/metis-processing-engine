package eu.europeana.processing.source;

import eu.europeana.processing.model.DataPartition;
import java.io.IOException;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.util.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDbEnumerator<S extends DbEnumeratorState> extends AbstractEnumerator<DataPartition,S> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDbEnumerator.class);


  protected AbstractDbEnumerator(SplitEnumeratorContext context,
      ParameterTool parameterTool, S state) {
    super(context, parameterTool, state);
  }

  protected void initEnumerator() {
    super.initEnumerator();
    recordsToBeProcessed = NOT_EVALUATED;
    LOGGER.info("Created enumerator with no fetched partitions");
  }

  protected void restoreEnumeratorFromState(S state) {
    super.restoreEnumeratorFromState(state);
    recordsToBeProcessed = state.getRecordsToBeProcessed();
    LOGGER.info(
        "Restored enumerator with finished: {} of: {} started, of {} records to be processed. Returned: {} partitions: {}",
        emittedRecordCount, startedRecordsCount, recordsToBeProcessed, returnedPartitions.size(), returnedPartitions);
  }

  @Override
  public void start() {
    super.start();
    if (recordsToBeProcessed == NOT_EVALUATED) {
      evaluateRecordsCount();
    } else {
      LOGGER.info("Record count already evaluated. Finished: {} of {} all records.",
          emittedRecordCount, recordsToBeProcessed);
    }
  }


  private void evaluateRecordsCount() {
    LOGGER.info("Preparing split count...");
    try {
      recordsToBeProcessed = countRecordsInDb();
      LOGGER.info("Finished, there is: {} records to be processed!", recordsToBeProcessed);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected DataPartition createNextPartition() {
    //TODO size of the split should be adjusted to parallelization level to work optimal
    //Is good to do the adjustment in some place.
    long partitionSize = Long.min(recordsToBeProcessed - startedRecordsCount, chunkSize);
    DataPartition partition = new DataPartition(startedRecordsCount, partitionSize, 0);
    startedRecordsCount += partitionSize;
    if (partitionSize > 0) {
      return partition;
    } else {
      return null;
    }
  }

  protected abstract long countRecordsInDb() throws IOException;

  @Override
  public S snapshotState(long checkpointId) {
    S state=super.snapshotState(checkpointId);
    state.setRecordsToBeProcessed(recordsToBeProcessed);
    return state;
  }
}
