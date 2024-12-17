package eu.europeana.processing.retryable;

/**
 * Exception used in retry mechanism
 */
public class RetryInterruptedException extends RuntimeException {

  public RetryInterruptedException(Throwable e) {
    super("Stopped waiting for retry, because the thread was interrupted!", e);
  }
}
