package eu.europeana.processing.exception;

public class RecoverableException extends FlinkWorkflowException {
    public RecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
    public RecoverableException(Throwable cause) {
        super(cause);
    }
}
