package eu.europeana.processing.http.reader;

import static java.util.Objects.requireNonNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractUnpackingTest {

  protected UUID enumeratorUuid = UUID.randomUUID();
  protected Path tempDirectory;

  @BeforeEach
  void createTempDir() throws IOException {
    tempDirectory = Files.createTempDirectory(getClass().getSimpleName());
  }

  @AfterEach
  public void deleteTempDir() throws IOException {
    FileUtils.deleteDirectory(tempDirectory.toFile());
  }

  protected Path copyFileToTempFolder(String name) throws IOException {
    Path resultFile = tempDirectory.resolve(name);
    IOUtils.copy(requireNonNull(HttpReaderTest.class.getResourceAsStream("/" + name)), new FileOutputStream(resultFile.toFile()));
    return resultFile;
  }

}
