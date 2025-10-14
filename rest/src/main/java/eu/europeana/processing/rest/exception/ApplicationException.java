package eu.europeana.processing.rest.exception;

/**
 * Very generic for now for simplicity purposes. In the future we should have more granular
 * exception handling
 */
public class ApplicationException extends Exception {

  public ApplicationException(Throwable cause) {
    super(cause);
  }


}
