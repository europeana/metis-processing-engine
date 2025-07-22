package eu.europeana.processing.exception;


public class UnrecoverableException extends FlinkWorkflowException {
    public UnrecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
    public UnrecoverableException(Throwable cause) {
        super(cause);
    }
}
