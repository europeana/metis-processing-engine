package eu.europeana.processing.exception;

public class FlinkWorkflowRuntimeException extends RuntimeException{
    public FlinkWorkflowRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
    public FlinkWorkflowRuntimeException(Throwable cause) {
        super(cause);
    }
}
