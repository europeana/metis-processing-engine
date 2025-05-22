package eu.europeana.cloud.flink.client.application;

public class ApplicationClusterException extends RuntimeException{

  public ApplicationClusterException(String message, Throwable cause) {
    super(message, cause);
  }

  public ApplicationClusterException(String message) {
    super(message);
  }
}
