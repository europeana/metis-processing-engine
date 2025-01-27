package eu.europeana.processing.http.source.dowloader;

import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.processing.http.source.HttpEnumerator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class is responsible for downloading archive file from the remote URL.
 */
public class HttpSourceFileDownloader {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpSourceFileDownloader.class);

  private final Path jobDirectoryPath;
  private final String archiveUrl;

  public HttpSourceFileDownloader(String jobDirectoryPath, String archiveUrl) {
    this.jobDirectoryPath = Path.of(jobDirectoryPath);
    this.archiveUrl = archiveUrl;
  }

  public Path download() {
    try {
      Path jobSharedFolder = createJobSharedFolder();
      LOGGER.info("Created job shared folder {}", jobSharedFolder);
      LOGGER.info("Starting http download from the url: {}", archiveUrl);
      HttpHarvester harvester = HarvesterFactory.createHttpHarvester();
      Path fileName = harvester.downloadFile(archiveUrl, jobSharedFolder);
      LOGGER.info("Downloaded file: {} from the url: {}", fileName, archiveUrl);
      return fileName;
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private Path createJobSharedFolder() throws IOException {
    return Files.createDirectory(jobDirectoryPath);
  }
}
