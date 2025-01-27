package eu.europeana.processing.http.source.extractor;

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
      throw new RuntimeException(e);
    }
  }

  public String getExtractedFileContent(String fileName) throws IOException {
    InputStream in = getFileInputStream(fileName);
    byte[] fileContent = IOUtils.toByteArray(in);
    return new String(fileContent, StandardCharsets.UTF_8);
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
