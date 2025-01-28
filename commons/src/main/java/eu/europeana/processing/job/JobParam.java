package eu.europeana.processing.job;

/**
 * List of all possible default values related with parallelism in jobs
 */
public class JobParam {

    public static final int DEFAULT_OPERATOR_PARALLELISM = 4;
    public static final int DEFAULT_READER_PARALLELISM = 4;
    public static final int DEFAULT_SINK_PARALLELISM = 4;
    public static final int DEFAULT_READER_MAX_RECORD_PENDING_COUNT = 100;

    private JobParam() {
    }
}
