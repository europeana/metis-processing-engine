package eu.europeana.cloud.flink.client;

import eu.europeana.processing.model.JobDetailsDto;
import eu.europeana.processing.model.JobSubmissionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class RestJobExecutor {


  private static final Logger LOGGER = LoggerFactory.getLogger(RestJobExecutor.class);

  private long WAIT_BEFORE_PROGRESS_CHECK_IN_MILLIS = 200;

  private static final long PROGRESS_PRINT_INTERVAL = 5;

  public JobDetailsDto execute(JobSubmissionDto jobSubmissionDto) throws InterruptedException {
    RestClient restClient = RestClient.builder()
                                      .baseUrl("http://127.0.0.1:8080")
                                      .build();

    JobDetailsDto jobDetailsDto = restClient.post()
                                            .uri("/jobs")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .body(jobSubmissionDto)
                                            .retrieve()
                                            .body(JobDetailsDto.class);

    System.out.println(jobDetailsDto);
    //wait for completion
    int i = 0;
    while (true) {
      try{
        Thread.sleep(WAIT_BEFORE_PROGRESS_CHECK_IN_MILLIS);

        JobDetailsDto result = restClient.get()
                                         .uri("/jobs/" + jobDetailsDto.taskId())
                                         .retrieve()
                                         .body(JobDetailsDto.class);
        if (++i % PROGRESS_PRINT_INTERVAL == 0) {
          LOGGER.info("Progress: {}", result.toString());
        }
        if (result.succeeded() > 0 || result.failed() > 0) {
          LOGGER.info("Job completed successfully!");
          return result;
        }
      }catch(Exception e){
        e.printStackTrace();
      }

    }
  }
}
