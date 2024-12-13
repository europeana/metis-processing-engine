#!/bin/bash
set -e
echo building flink-poc docker image that can be deployed on the openshift cluster.

#images
cd flink_java21
echo building :: flink java 21 base $(pwd)
docker build --no-cache -t flink:1.18.1-java21_poc .
cd ..
cd flink-node
echo building :: flink-node $(pwd)
docker build --no-cache -t metis-processing-engine-flink:1.18.1-java21 .

