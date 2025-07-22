package eu.europeana.processing.exception;

public class FlinkWorkflowException extends Exception{
    public FlinkWorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
    public FlinkWorkflowException(Throwable cause) {
        super(cause);
    }
}
