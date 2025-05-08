package eu.europeana.processing.blocking;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import org.apache.flink.shaded.guava31.com.google.common.cache.Cache;
import org.apache.flink.shaded.guava31.com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prevents running two job instances of the job at the same time on job manager. Flink does not prevent such a situation during
 * job restart, because executes org.apache.flink.api.connector.source.SplitEnumerator#close() asynchronously and do not wait for
 * the method execution finish. In fact, even executions in Flink job thread by:
 * org.apache.flink.api.connector.source.SplitEnumeratorContext#runInCoordinatorThread(java.lang.Runnable) are not prevented,
 * while at the same time Flink starts the second instance of the restarted job. These could lead to errors caused by the two
 * instances of the same job doing some things at the same time.
 */
public final class BlockingService {

  private static final int LOCK_WAITING_TIME_BEFORE_JOB_MANAGER_RESTART_MINUTES = 10;

  private static final Logger LOGGER = LoggerFactory.getLogger(BlockingService.class);

  /**
   * Size of the cache. The value which is big enough that the semaphore for longest running task would never be removed from
   * the cache.
   */
  private static final Cache<String, Semaphore> semaphores = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofDays(90))
                                                                         .build();

  private BlockingService() {
  }

  /**
   * Acquires lock for the given job execution (identified by jobUuid). If the lock could not be acquired instantly, the
   * method waits 10 minutes and after that time exits JobManager JMV. This results that previous job instance that holds the
   * lock is removed together with the JVM and after JobManager restart, new job instance should be properly started.
   *
   * @param jobUuid - unique id for the job execution
   * @throws InterruptedException - if the job execution is interrupted during waiting for the lock.
   * @throws ExecutionException - if the new Semaphore could not be created, what should never happen in practice
   */
  @SuppressWarnings("java:S1147")
  public static void aquireLock(String jobUuid) throws InterruptedException, ExecutionException {
    Semaphore semaphore = semaphores.get(jobUuid, () -> new Semaphore(1));
    if (semaphore.availablePermits() == 0) {
      LOGGER.warn("Waiting for lock for the job (uuid: {}) ...", jobUuid);
    }
    if (!semaphore.tryAcquire(LOCK_WAITING_TIME_BEFORE_JOB_MANAGER_RESTART_MINUTES, MINUTES)) {
      LOGGER.error("FATAL could not obtain the job (uuid: {}) lock in the expected time. Killing job manager!", jobUuid);
      System.exit(1);
    }
    LOGGER.warn("Acquired lock for the the job (uuid: {}) ...", jobUuid);
  }

  /**
   * Releases the lock for given job execution
   *
   * @param jobUuid - unique id for the job execution
   */
  public static void releaseLock(String jobUuid) {
    Semaphore semaphore = semaphores.getIfPresent(jobUuid);
    if (semaphore == null) {
      throw new IllegalStateException("There is no lock for jobUuid: " + jobUuid + "!");
    }
    semaphore.release();
  }

}
