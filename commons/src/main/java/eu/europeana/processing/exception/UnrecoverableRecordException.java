package eu.europeana.processing.exception;



public class UnrecoverableRecordException extends UnrecoverableException {
    public UnrecoverableRecordException(String message, Throwable cause) {
        super(message, cause);
    }
    public UnrecoverableRecordException(Throwable cause) {
        super(cause);
    }
}