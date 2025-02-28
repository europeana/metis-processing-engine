package eu.europeana.cloud.flink.client.exceptions;

public class SubmitJobException extends RuntimeException{
    public SubmitJobException(Throwable e) {
        super("Exception occurred during job submission process", e);
    }

    public SubmitJobException(String message) {
        super(message);
    }

    public SubmitJobException(String message, Throwable e) {
        super(message, e);
    }
}
