package eu.europeana.processing.exception;



public class UnrecoverableJobException extends UnrecoverableException {
    public UnrecoverableJobException(String message, Throwable cause) {
        super(message, cause);
    }
    public UnrecoverableJobException(Throwable cause) {
        super(cause);
    }
}