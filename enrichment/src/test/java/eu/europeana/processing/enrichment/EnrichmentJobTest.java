package eu.europeana.processing.enrichment;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.http.JvmProxyConfigurer;
import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.test.common.AbstractJobTest;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;


@Disabled("It was not finished and will be finished as part of ticket: MET-6675")
@ExtendWith(MockitoExtension.class)
class EnrichmentJobTest extends AbstractJobTest {

  private static WireMockServer wireMockServer;

  @BeforeAll
  static void createWireMock() throws IOException {
    wireMockServer = new WireMockServer(wireMockConfig()
        .dynamicPort()
        .enableBrowserProxying(true)
        .notifier(new ConsoleNotifier(true)));
    wireMockServer.start();
    JvmProxyConfigurer.configureFor(wireMockServer);
    mockEnrichmentWeb();
  }

  @Test
  void shouldProperlyRunEnrichment() throws Exception {
    startPostgresDbServer("enrichment-input.sql");
    String[] args = prepareArgs(
        "--" + JobParamName.DEREFERENCE_SERVICE_URL, "http://localhost:" + wireMockServer.port() ,
        "--" + JobParamName.ENRICHMENT_ENTITY_MANAGEMENT_URL, "http://localhost:" + wireMockServer.port() + "/entitymgmt",
        "--" + JobParamName.ENRICHMENT_ENTITY_API_URL, "http://localhost:" + wireMockServer.port() + "/entity",
        "--" + JobParamName.ENRICHMENT_ENTITY_API_TOKEN_ENDPOINT, "entityApiGrantParams",
        "--" + JobParamName.ENRICHMENT_ENTITY_API_GRANT_PARAMS, "test-api-grant-params");

    EnrichmentJob.main(args);

    assertThatResultRowIsSavedInDb();
    assertNoErrorsSavedInDb();
  }

  @Test
  void shouldSaveRecordErrorInDB() throws Exception {
    startPostgresDbServer("invalid-record-input.sql");
    String[] args = prepareArgs(
        "--" + JobParamName.DEREFERENCE_SERVICE_URL, "http://localhost:" + wireMockServer.port() ,
        "--" + JobParamName.ENRICHMENT_ENTITY_MANAGEMENT_URL, "http://localhost:" + wireMockServer.port() + "/entitymgmt",
        "--" + JobParamName.ENRICHMENT_ENTITY_API_URL, "http://localhost:" + wireMockServer.port() + "/entity",
        "--" + JobParamName.ENRICHMENT_ENTITY_API_TOKEN_ENDPOINT, "entityApiGrantParams",
        "--" + JobParamName.ENRICHMENT_ENTITY_API_GRANT_PARAMS, "test-api-grant-params");

    EnrichmentJob.main(args);

    assertThatErrorIsSavedInDb();
  }

  @Override
  protected int stepNumber() {
    return 6;
  }

  private static void mockEnrichmentWeb() throws IOException {
    wireMockServer.stubFor(
        get(urlEqualTo("/dereference?uri=http%3A%2F%2Fdata.europeana.eu%2Ftimespan%2F20"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(getResourceFileContent("entity-api/entity-api-response-timespan.json"))
                .withStatus(HttpStatus.OK.value())));

  }

   

  private static String getResourceFileContent(String fileName) throws IOException {
    try (InputStream resourceStream = EnrichmentJobTest.class.getClassLoader().getResourceAsStream(fileName)) {
      return new String(resourceStream.readAllBytes());
    }
  }

}