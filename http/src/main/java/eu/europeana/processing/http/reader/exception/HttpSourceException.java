package eu.europeana.processing.http.reader.exception;

/**
 * The exception thrown by http source on general errors.
 */
public class HttpSourceException extends RuntimeException{

  public HttpSourceException(String message) {
    super(message);
  }

  public HttpSourceException(String message, Throwable cause) {
    super(message, cause);
  }
}
