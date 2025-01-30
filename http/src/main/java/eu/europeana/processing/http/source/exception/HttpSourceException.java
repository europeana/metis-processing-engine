package eu.europeana.processing.http.source.exception;

/**
 * The exception thrown by http source on general errors.
 */
public class HttpSourceException extends RuntimeException{

  public HttpSourceException(String message) {
    super(message);
  }

  public HttpSourceException(String message, Exception exception) {
    super(message, exception);
  }
}
