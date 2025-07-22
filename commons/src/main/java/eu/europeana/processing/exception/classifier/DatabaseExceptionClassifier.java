package eu.europeana.processing.exception.classifier;


import eu.europeana.processing.exception.FlinkWorkflowException;
import eu.europeana.processing.exception.RecoverableException;
import eu.europeana.processing.exception.UnrecoverableJobException;
import eu.europeana.processing.exception.UnrecoverableRecordException;

import java.sql.SQLException;

public class DatabaseExceptionClassifier {

    private DatabaseExceptionClassifier() {}

    public static FlinkWorkflowException classify(SQLException sqlException) {
        String sqlState = sqlException.getSQLState();
        if (sqlState == null || sqlState.length() < 2) {
            return new UnrecoverableJobException("Unknown database error", sqlException);
        }

//        08	Connection Exception	ConnectionException
//        22	Data Exception (invalid data)	DataFormatException
//        23	Integrity Constraint Violation	IntegrityConstraintException
//        28	Invalid Authorization	AuthenticationException
//        40	Transaction Rollback	TransactionException
//        42	Syntax Error or Access Rule Violation	SqlSyntaxException

        return switch (sqlState.substring(0, 2)) {
            case "08" -> new RecoverableException("Connection error", sqlException);
            case "22" -> new UnrecoverableRecordException("Invalid data format", sqlException);
            case "23", "28", "40", "42" -> new UnrecoverableJobException("Database error (" + sqlException.getSQLState() + ")", sqlException);
            default -> new UnrecoverableJobException("Database error", sqlException);
        };
    }

}
