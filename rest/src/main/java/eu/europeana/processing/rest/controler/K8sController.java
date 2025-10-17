package eu.europeana.processing.rest.controler;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created just to test the communication with k8s
 */
@RestController
@RequestMapping("k8s")
public class K8sController {

  private static final Logger LOGGER = LoggerFactory.getLogger(K8sController.class);

  private final CoreV1Api coreV1Api;

  /**
   * Constructor
   *
   * @param coreV1Api {@link CoreV1Api}
   */
  public K8sController(CoreV1Api coreV1Api) {
    this.coreV1Api = coreV1Api;
  }

  @GetMapping("/")
  public List<V1Pod> getPods() throws ApiException {
    LOGGER.info("Getting Pods from K8s API");
    return coreV1Api.listNamespacedPod("europeana-processing-engine-acceptance").execute().getItems();
  }

}
