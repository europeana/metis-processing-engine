package eu.europeana.processing.http.reader.dowloader;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.processing.http.reader.exception.HttpSourceException;
import java.io.IOException;
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

  /**
   * Creates HttpSourceFileDownloader
   *
   * @param jobDirectoryPath - directory where the file should be downloaded
   * @param archiveUrl - url of the archive file which will be downloaded
   */
  public HttpSourceFileDownloader(String jobDirectoryPath, String archiveUrl) {
    this.jobDirectoryPath = Path.of(jobDirectoryPath);
    this.archiveUrl = archiveUrl;
  }

  /**
   * Download a file from the url into the directory
   * @return path to the downloaded file
   */
  public Path download() {
    try {
      createJobSharedFolder();
      LOGGER.debug("Created job shared folder {}", jobDirectoryPath);
      LOGGER.debug("Starting http download from the url: {}", archiveUrl);
      HttpHarvester harvester = HarvesterFactory.createHttpHarvester();
      Path fileName = harvester.downloadFile(archiveUrl, jobDirectoryPath);
      LOGGER.info("Downloaded file: {} from the url: {}", fileName, archiveUrl);
      return fileName;
    } catch (IOException | HarvesterException e) {
      throw new HttpSourceException("Could not download archive file: " + archiveUrl, e);
    }
  }

  private void createJobSharedFolder() throws IOException {
    if(!Files.exists(jobDirectoryPath)) {
      Files.createDirectory(jobDirectoryPath);
    }
  }
}
