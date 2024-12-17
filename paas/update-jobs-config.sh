#!/bin/bash
. .general_settings.sh

set -e
echo Deploying Flink on the openshift cluster. $(oc project)
#checikng if project is valid
echo Checking project...
oc project | grep $OPENSHIFT_PROJECT
echo Project OK

set +e
oc delete secret jobs-config
set -e
oc create secret generic jobs-config --from-file=$FLINK_CONFIG_DIR/$FLINK_ENV/jobs
