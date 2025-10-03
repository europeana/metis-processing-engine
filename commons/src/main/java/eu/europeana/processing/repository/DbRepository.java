package eu.europeana.processing.repository;

import java.io.Closeable;

/**
 * Base abstract interface of all the repositories
 */
public interface DbRepository {

  /**
   * Shutdowns the repository what usually means closes all the resources used by the repository.
   * {@link Closeable#close()} or {@link AutoCloseable#close()} was not used by purpose. It may suggest
   * that repository method executions should be used with try-with-resources construction. It cannot
   * be done because connection pool would be closed in this case.
   */
  void shutdown();
}
