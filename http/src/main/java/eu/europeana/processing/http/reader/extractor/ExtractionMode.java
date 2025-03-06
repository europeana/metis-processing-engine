package eu.europeana.processing.http.reader.extractor;

/**
 * Mode of extraction.
 */
public enum ExtractionMode {

  /**
   * Used only for zip files without embedded archives files in it. The compresed files from the archive
   * are extracted directly to the memory during execution of particular record.
   */
  ON_FLY_IN_MEMORY,

  /**
   * Used for all other archives, the file is extracted to the shared folder, at beginning of the execution.
   */
  INITIAL_TO_DIRECTORY
}
