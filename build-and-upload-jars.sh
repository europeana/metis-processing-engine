. config/jar-upload-config.sh

startTime=$(date +"%Y-%m-%dT%H:%M:%S%Z")
jarProperties="Properties configuration:";

upload-jar(){
  local jarPropertyName=$1
  local filePath=$2
  echo "Uploading jar: $filePath ..."
  resultJson=$(curl -u ${FLINK_API_USER}:${FLINK_API_PASSWORD} -F file=@"$filePath" ${FLINK_API_URL}/jars/upload)
  echo "Upload result: $resultJson"
  status=$(echo $resultJson | jq -r '.status')
  jarPath=$(echo $resultJson | jq -r '.filename')
  jarId=$(basename "$jarPath")
  if [[ "$status" = "success" ]]; then
    jarProperty="$jarPropertyName=$jarId"
    echo "Successfully Uploaded jar: $jarProperty"
    jarProperties+=$'\n'"$jarProperty"
  fi
}

set -e
mvn clean install -DskipTests
buildTime=$(date +"%Y-%m-%dT%H:%M:%S%Z")

#To upload select jars comment the lines starting from upload-jar (below)
upload-jar "flink.jar.id.oai" "oai/target/metis-processing-engine-oai-$FLINK_JAR_VERSION.jar"
upload-jar "flink.jar.id.validation" "validation/target/metis-processing-engine-validation-$FLINK_JAR_VERSION.jar"
upload-jar "flink.jar.id.transformation" "transformation/target/metis-processing-engine-transformation-$FLINK_JAR_VERSION.jar"
upload-jar "flink.jar.id.normalization" "normalization/target/metis-processing-engine-normalization-$FLINK_JAR_VERSION.jar"
upload-jar "flink.jar.id.enrichment" "enrichment/target/metis-processing-engine-enrichment-$FLINK_JAR_VERSION.jar"
upload-jar "flink.jar.id.media" "media/target/metis-processing-engine-media-$FLINK_JAR_VERSION.jar"
upload-jar "flink.jar.id.indexing" "indexing/target/metis-processing-engine-indexing-$FLINK_JAR_VERSION.jar"

echo "Upload of all flink jars finished!"
echo "$jarProperties"
echo -------------------------------------------------------------------------------
echo -e '\033[1;32m'DEPLOY SUCCESSFULL'\033[0m'
echo -------------------------------------------------------------------------------
echo Flink job jars were successfuly deployed on the openshift PaaS server
echo Started at: $startTime
echo Build at: $buildTime
echo Finished at: $(date +"%Y-%m-%dT%H:%M:%S%Z")
echo -------------------------------------------------------------------------------

