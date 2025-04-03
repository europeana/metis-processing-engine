#!/bin/bash
set -e
echo building flink-poc docker image that can be deployed on the openshift cluster.

#images
echo "building :: flink java 21 base $(realpath flink-java21)"
docker build --no-cache -t flink:2.0.0-java21_poc flink-java21

echo "building :: flink-node $(realpath flink-with-tools)"
docker build --no-cache -t flink-with-tools:2.0.0-java21 flink-with-tools

echo "building :: shared libs: $(realpath metis-processing-engine-flink/shared-libs)"
mvn -f metis-processing-engine-flink/shared-libs clean install
echo "building :: flink-node $(realpath metis-processing-engine-flink)"
docker build --no-cache -t metis-processing-engine-flink:2.0.0-java21 metis-processing-engine-flink

#echo "Pushing to the repository: registry.paas.psnc.pl"
#docker tag metis-processing-engine-flink:2.0.0-java21 registry.paas.psnc.pl/ecloud-poc/metis-processing-engine-flink:2.0.0-java21
#docker push registry.paas.psnc.pl/ecloud-poc/metis-processing-engine-flink:2.0.0-java21

