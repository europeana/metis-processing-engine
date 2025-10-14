package eu.europeana.processing.rest.validation;

import java.util.Map;

public interface JobParamsValidator {


  boolean validate(Map<String, String> parameters);
}
