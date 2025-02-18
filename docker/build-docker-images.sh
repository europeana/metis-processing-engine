#!/bin/bash
set -e
echo building flink-poc docker image that can be deployed on the openshift cluster.

#images
cd flink-java21
echo building :: flink java 21 base $(pwd)
docker build --no-cache -t flink:1.20.0-java21_poc .
cd ..

cd flink-with-tools
echo building :: flink-node $(pwd)
docker build --no-cache -t flink-with-tools:1.20.0-java21 .
cd ..

cd metis-processing-engine-flink/shared-libs
mvn clean install
cd ..
echo building :: flink-node $(pwd)
docker build --no-cache -t metis-processing-engine-flink:1.20.0-java21 .
cd ..


#docker tag metis-processing-engine-flink:1.20.0-java21 registry.paas.psnc.pl/ecloud-poc/metis-processing-engine-flink:1.20.0-java21
#docker push registry.paas.psnc.pl/ecloud-poc/metis-processing-engine-flink:1.20.0-java21

