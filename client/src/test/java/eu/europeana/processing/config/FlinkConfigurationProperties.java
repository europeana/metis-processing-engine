package eu.europeana.processing.config;

import java.util.stream.IntStream;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "flink")
public class FlinkConfigurationProperties {
  String jobManagerUrl;
  String jobManagerUser;
  String jobManagerPassword;
  int readerParallelism;
  int operatorParallelism;
  int sinkParallelism;
  int maxRecordPending;
  int chunkSize;

  public int getMaxParallelism() {
    return IntStream.of(readerParallelism, operatorParallelism, sinkParallelism).max().getAsInt();
  }
}
