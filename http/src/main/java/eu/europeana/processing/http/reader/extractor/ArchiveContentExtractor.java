package eu.europeana.processing.http.reader.extractor;

import eu.europeana.processing.http.reader.exception.HttpSourceException;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;

/**
 * Class responsible for providing content of the record files that are contained in the downloaded
 * archive file. Depending on the extractionMode:
 * <li>It extracts files from the zip file directly to the memory</li>
 * <li>Reads files from the folder extracted earlier on the previous stage</li>
 */
public class ArchiveContentExtractor implements Closeable {

  private final ExtractionMode extractionMode;
  private ZipFile zipFile;

  /**
   * Creates ArchiveContentExtractor
   *
   * @param extractionMode - extraction mode - null is passed normally and not null is passed only while
   * restoring the job state from checkpoint.
   * @param archivePath - path to the archive file downloaded to the shared folder.
   */
  public ArchiveContentExtractor(ExtractionMode extractionMode, String archivePath) {
    this.extractionMode = extractionMode;
    if (extractionMode == ExtractionMode.ON_FLY_IN_MEMORY) {
      openZipFile(archivePath);
    }
  }

  private void openZipFile(String archivePath) {
    try {
      zipFile = ZipFile.builder().setPath(archivePath).get();
    } catch (IOException e) {
      throw new HttpSourceException("Could not open zip file: " + archivePath, e);
    }
  }

  /**
   * Returns the extracted content of the given file
   * @param fileName - depending on the mode, relative name with path, of the file in the archive, or
   * path to the file in the shared folder
   * @return extracted file content as string
   * @throws HttpSourceException - while could not get file content: by extract from zip or read from extracted directory
   */
  public String getExtractedFileContent(String fileName) throws HttpSourceException {
    try {
      InputStream in = getFileInputStream(fileName);
      byte[] fileContent = IOUtils.toByteArray(in);
      return new String(fileContent, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new HttpSourceException("Could not get compressed content of the file: " + fileName, e);
    }
  }

  private InputStream getFileInputStream(String fileName) throws IOException {
    return switch (extractionMode) {
      case ON_FLY_IN_MEMORY -> getFileFromZipInputStream(fileName);
      case INITIAL_TO_DIRECTORY -> getFileFromExtractedDirectory(fileName);
    };
  }

  private InputStream getFileFromZipInputStream(String fileName) throws IOException {
    return zipFile.getInputStream(zipFile.getEntry(fileName));
  }

  private InputStream getFileFromExtractedDirectory(String fileName) throws IOException {
    return new FileInputStream(fileName);
  }

  @Override
  public void close() throws IOException {
    if (zipFile != null) {
      zipFile.close();
    }
  }
}
