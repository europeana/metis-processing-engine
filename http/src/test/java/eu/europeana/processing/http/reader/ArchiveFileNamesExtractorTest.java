package eu.europeana.processing.http.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.europeana.processing.http.reader.extractor.ArchiveFileNamesExtractor;
import eu.europeana.processing.http.reader.extractor.ExtractionMode;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


class ArchiveFileNamesExtractorTest extends AbstractUnpackingTest {

  @ParameterizedTest
  @CsvSource({
      "gzFile.tar.gz,INITIAL_TO_DIRECTORY,13",
      "gzFilesWithMixedCompressedFiles.tar.gz,INITIAL_TO_DIRECTORY,13",
      "gzFileWithCompressedGZFiles.tar.gz,INITIAL_TO_DIRECTORY,13",
      "gzFileWithCompressedGZFiles.tgz,INITIAL_TO_DIRECTORY,13",
      "records.zip,ON_FLY_IN_MEMORY,4",
      "zipFileFromMac.zip,ON_FLY_IN_MEMORY,26",
      "ZipFilesWithMixedCompressedFiles.zip,INITIAL_TO_DIRECTORY,13",
      "zipFileTest.tar.gz,INITIAL_TO_DIRECTORY,2",
      "zipFileTest.zip,INITIAL_TO_DIRECTORY,2",
      "zipFileWithNestedFolders.zip,INITIAL_TO_DIRECTORY,13",
      "zipFileWithNestedZipFiles.zip,INITIAL_TO_DIRECTORY,13",
      "zipWithCorrectAndCorruptedEDM.zip,ON_FLY_IN_MEMORY,2",
      "zipWithCorruptedEDM.zip,ON_FLY_IN_MEMORY,1",
      "zipWithEDMs.zip,ON_FLY_IN_MEMORY,1"
  })
  void shouldExtractFileNamesFromDifferentArchives(String fileName, ExtractionMode expectedExtractionMode, int expectedFileCount)
      throws IOException {
    Path filePath = copyFileToTempFolder(fileName);
    ArchiveFileNamesExtractor extractor = new ArchiveFileNamesExtractor(filePath, null);

    ExtractionMode mode = extractor.extract();

    assertEquals(expectedExtractionMode, mode);
    assertEquals(expectedFileCount, extractor.getFileNames().size());
  }

  @Test
  void shouldThrowExceptionOnNonZipFile()
      throws IOException {
    Path filePath = copyFileToTempFolder("empty.zip");
    ArchiveFileNamesExtractor extractor = new ArchiveFileNamesExtractor(filePath, null);

    assertThrows(Exception.class, extractor::extract);
  }

}