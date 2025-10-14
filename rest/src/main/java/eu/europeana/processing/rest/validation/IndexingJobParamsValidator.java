package eu.europeana.processing.rest.validation;

import eu.europeana.processing.job.JobParamName;
import java.util.Map;

/**
 * Validates parameters provided in {@link eu.europeana.processing.rest.dto.JobSubmissionDto} for indexing job
 */

public class IndexingJobParamsValidator implements JobParamsValidator {

  @Override
  public boolean validate(Map<String, String> parameters) {
    return parameters.containsKey(JobParamName.DATASET_ID) &&
        parameters.containsKey(JobParamName.VALIDATION_TYPE) &&
        parameters.containsKey(JobParamName.INDEXING_PRESERVETIMESTAMPS) &&
        parameters.containsKey(JobParamName.INDEXING_PERFORMREDIRECTS) &&
        parameters.containsKey(JobParamName.INDEXING_MONGOINSTANCES) &&
        parameters.containsKey(JobParamName.INDEXING_MONGOPORTNUMBER) &&
        parameters.containsKey(JobParamName.INDEXING_MONGODBNAME) &&
        parameters.containsKey(JobParamName.INDEXING_MONGOREDIRECTDBNAME) &&
        parameters.containsKey(JobParamName.INDEXING_MONGOUSERNAME) &&
        parameters.containsKey(JobParamName.INDEXING_MONGOPASSWORD) &&
        parameters.containsKey(JobParamName.INDEXING_MONGOAUTHDB) &&
        parameters.containsKey(JobParamName.INDEXING_MONGOUSESSL) &&
        parameters.containsKey(JobParamName.INDEXING_MONGOREADPREFERENCE) &&
        parameters.containsKey(JobParamName.INDEXING_MONGOPOOLSIZE) &&
        parameters.containsKey(JobParamName.INDEXING_SOLRINSTANCES) &&
        parameters.containsKey(JobParamName.INDEXING_ZOOKEEPERINSTANCES) &&
        parameters.containsKey(JobParamName.INDEXING_ZOOKEEPERPORTNUMBER) &&
        parameters.containsKey(JobParamName.INDEXING_ZOOKEEPERCHROOT) &&
        parameters.containsKey(JobParamName.INDEXING_ZOOKEEPERDEFAULTCOLLECTION) &&
        parameters.containsKey(JobParamName.INDEXING_MONGOAPPLICATIONNAME);
  }
}
