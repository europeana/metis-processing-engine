package eu.europeana.processing.config;

/**
 * Mode of cleaning DB before tests execution.
 */
public enum DbCleaningMode {
    /**
     * Official test mode. Clears all the unnecessary data from the DB with exceptions of previous
     * workflow steps from which data are needed to complete given test. In details:
     * 1. For first workflow step - harvesting it cleans (truncate) the whole DB, all the datasets.
     * 2. For next workflow steps it cleans all the data in DB except previous steps on the current testing dataset.
     * So for other than test datasets it cleans all the data also previous steps.
     * For example for transformation it left only harvesting and external validation data of the
     * current dataset. It completely cleans all other datasets.
     */
    CLEAN_ALL_DB_BUT_PREVIOUS_STEPS_OF_TEST_DATASET,

    /**
     * The same as previous but it does not touch datasets other than currently tested.
     * So for first workflow step (harvesting) it cleans all the current dataset.
     * For further workflow steps it cleans only current and the following steps of the current dataset.
     */
    CLEAN_CURRENT_AND_FOLLOWING_STEPS_OF_TEST_DATASET,

    /**
     * No cleaning. Usefully only for special occasions cause test reuse task_id and existing data
     * from previous tests, left in DB could influence testing.
     */
    NO_CLEANING


}
