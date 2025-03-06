package eu.europeana.processing.http.reader.extractor;

import static java.util.Collections.unmodifiableList;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration.IterationResult;
import eu.europeana.metis.harvesting.http.PathIterator;
import eu.europeana.metis.utils.CompressedFileExtension;
import eu.europeana.metis.utils.CompressedFileHandler;
import eu.europeana.processing.http.reader.exception.HttpSourceException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.flink.shaded.curator5.com.google.common.collect.Streams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class is responsible for extracting list of the files from an archive. For zip archive file without nested archive files
 * inside, only a zip file header containing names of compressed files is read. In other cases, the archive is extracted into a
 * folder and list of paths of the extracted files is returned. This extracted folder is then reused on further steps.
 */
public class ArchiveFileNamesExtractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveFileNamesExtractor.class);
  public static final String EXTRACTED_SUB_DIR_NAME = "extracted";

  private final Path downloadedFile;
  private final Path extractedDirectory;
  private ExtractionMode extractionMode;
  private List<String> zippedFileNamesList;


  /**
   * Creates ArchiveFileNamesExtractor.
   *
   * @param downloadedFile - path to the downloaded archive file
   * @param extractionMode - mode of the extraction. Null value is allowed and passed on first execution.
   * Not null extraction mode could be passed during state restoring, so the extraction of the archive
   * to the folder used in mode: INITIAL_TO_DIRECTORY could be omitted.
   */
  public ArchiveFileNamesExtractor(Path downloadedFile, ExtractionMode extractionMode) {
    this.downloadedFile = downloadedFile;
    this.extractedDirectory = downloadedFile.toAbsolutePath().getParent().resolve(EXTRACTED_SUB_DIR_NAME);
    this.extractionMode = extractionMode;
  }

  /**
   * Extracts list of compressed files names and choses extraction mode.
   *
   * @return - extraction mode
   */
  public ExtractionMode extract() {
    if (extractionMode == null) {
      try {
        extractionMode = extractFileNamesFromArchive();
      } catch (IOException e) {
        throw new HttpSourceException("Cound not extract compressed files names from the zip archive header of the file: " + downloadedFile, e);
      }
    } else {
      LOGGER.debug("Need not to extract. Extraction already performed. Extraction mode: {}", extractionMode);
    }
    return extractionMode;
  }

  private ExtractionMode extractFileNamesFromArchive() throws IOException {
    CompressedFileExtension compressingExtension = CompressedFileExtension.forPath(downloadedFile);
    if (compressingExtension == CompressedFileExtension.ZIP) {
      if (zipContainsOnlyExtractedFiles()) {
        LOGGER.info("Zip file contains only extracted files. Chosen extraction mode: {}", ExtractionMode.ON_FLY_IN_MEMORY);
        return ExtractionMode.ON_FLY_IN_MEMORY;
      } else {
        LOGGER.info("Zip file contains nested archives. Chosen extraction mode: {}", ExtractionMode.INITIAL_TO_DIRECTORY);
      }
    } else {
      LOGGER.info("Chosen extraction mode: {} for file of type: {}", ExtractionMode.INITIAL_TO_DIRECTORY, compressingExtension);
    }
    extractFilesToDirectory();
    return ExtractionMode.INITIAL_TO_DIRECTORY;
  }

  public List<String> getFileNames() {
    try {
      return switch (extractionMode) {
        case ON_FLY_IN_MEMORY -> getNamesFromZipHeader();
        case INITIAL_TO_DIRECTORY -> getExtractedFilePaths();
      };
    } catch (HarvesterException | IOException e) {
      throw new HttpSourceException("Could not gather file name list from archive file: " + downloadedFile, e);
    }
  }

  private List<String> getNamesFromZipHeader() throws IOException {
    //The result is stored in the field to not extract zip header twice.
    if (zippedFileNamesList == null) {
      LOGGER.debug("Zip archive file type. Reading header...");
      try (ZipFile zipFile = ZipFile.builder().setPath(downloadedFile).get()) {
        zippedFileNamesList = Streams.stream(zipFile.getEntries().asIterator())
                                     .filter(entry -> !entry.isDirectory())
                                     .map(ZipArchiveEntry::getName)
                                     .toList();
      }
    }
    return unmodifiableList(zippedFileNamesList);
  }

  private boolean zipContainsOnlyExtractedFiles() throws IOException {
    return getNamesFromZipHeader().stream().noneMatch(CompressedFileExtension::hasCompressedFileExtension);
  }

  private void extractFilesToDirectory() throws IOException {
    LOGGER.debug("Creating extracted dir: {}", extractedDirectory);
    Files.createDirectory(extractedDirectory);
    LOGGER.debug("Extracting the archive: {}", downloadedFile);
    CompressedFileHandler.extractFile(downloadedFile, extractedDirectory);
    LOGGER.info("The archive file successfully extracted into the directory: {}", extractedDirectory);
  }

  @SuppressWarnings("resource") //We could not close iterator cause it removes extracted folder
  private List<String> getExtractedFilePaths() throws HarvesterException {
    List<String> namesList = new ArrayList<>();
    new PathIterator(extractedDirectory).forEach(path -> {
      namesList.add(path.toString());
      return IterationResult.CONTINUE;
    });
    return namesList;
  }
}
