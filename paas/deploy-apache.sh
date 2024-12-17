#!/bin/bash
. .general_settings.sh

set -e
echo Checking project...
oc project | grep $OPENSHIFT_PROJECT
echo Project OK

set +e
oc delete secret htpasswd
oc delete configmap apache-dashboard
oc delete configmap apache-mod-security
set -e
oc create secret generic htpasswd --from-file=$FLINK_CONFIG_DIR/$FLINK_ENV/apache/.httpasswd
oc create configmap apache-dashboard --from-file=$FLINK_CONFIG_DIR/common/apache/dashboard.conf
oc create configmap apache-mod-security --from-file=$FLINK_CONFIG_DIR/common/apache/mod_security.conf

oc apply -f apache/apache.yaml
oc apply -f apache/apache-service.yaml
oc apply -f $FLINK_CONFIG_DIR/$FLINK_ENV/apache/apache-route.yaml
echo Deploy completed - OK
