#!/bin/bash
set -e
echo Redeploying flink-poc on the openshift cluster. $(oc project)
#checikng if project is valid
echo Checking project...
oc project | grep ecloud-flink-poc
echo Project OK

# Remove the deployments for the cluster
oc delete -f flink/job-manager-session-deployment.yaml
oc delete -f flink/task-manager-session-deployment.yaml

# Remove client
oc delete -f flink/flink-client.yaml

# Create the deployments for the cluster
oc apply -f flink/job-manager-session-deployment.yaml
oc apply -f flink/task-manager-session-deployment.yaml

# Create client
oc apply -f flink/flink-client.yaml
