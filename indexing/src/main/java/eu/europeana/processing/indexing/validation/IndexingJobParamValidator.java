package eu.europeana.processing.indexing.validation;

import eu.europeana.processing.job.JobParamName;
import eu.europeana.processing.validation.AbstractInternalSourceJobValidator;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Validates parameters provided for {@link eu.europeana.processing.indexing.IndexingJob} during task startup.
 */
public class IndexingJobParamValidator extends AbstractInternalSourceJobValidator {


  @Override
  public void validateJobSpecificParameters(ParameterTool parameterTool) {
    parameterTool.getRequired(JobParamName.INDEXING_PRESERVETIMESTAMPS);
    parameterTool.getRequired(JobParamName.INDEXING_PERFORMREDIRECTS);

    parameterTool.getRequired(JobParamName.INDEXING_MONGOINSTANCES);
    parameterTool.getRequired(JobParamName.INDEXING_MONGOPORTNUMBER);
    parameterTool.getRequired(JobParamName.INDEXING_MONGODBNAME);
    parameterTool.getRequired(JobParamName.INDEXING_MONGOREDIRECTDBNAME);
    parameterTool.getRequired(JobParamName.INDEXING_MONGOUSERNAME);
    parameterTool.getRequired(JobParamName.INDEXING_MONGOPASSWORD);
    parameterTool.getRequired(JobParamName.INDEXING_MONGOAUTHDB);
    parameterTool.getRequired(JobParamName.INDEXING_MONGOUSESSL);
    parameterTool.getRequired(JobParamName.INDEXING_MONGOREADPREFERENCE);
    parameterTool.getRequired(JobParamName.INDEXING_MONGOPOOLSIZE);
    parameterTool.getRequired(JobParamName.INDEXING_SOLRINSTANCES);
    parameterTool.getRequired(JobParamName.INDEXING_ZOOKEEPERINSTANCES);
    parameterTool.getRequired(JobParamName.INDEXING_ZOOKEEPERPORTNUMBER);
    parameterTool.getRequired(JobParamName.INDEXING_ZOOKEEPERCHROOT);
    parameterTool.getRequired(JobParamName.INDEXING_ZOOKEEPERDEFAULTCOLLECTION);
    parameterTool.getRequired(JobParamName.INDEXING_MONGOAPPLICATIONNAME);
  }
}
