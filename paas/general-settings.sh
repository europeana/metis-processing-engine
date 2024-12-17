#!/bin/bash
set -e

if [ -z ${FLINK_ENV} ]; then echo Error: Environment variable FLINK_ENV not defined; exit 1; fi

if [ -z ${FLINK_CONFIG_DIR} ]; then echo Error: Environment variable FLINK_CONFIG_DIR not defined; exit 1; fi

if [ ${FLINK_ENV} = "test" ]; then export OPENSHIFT_PROJECT="ecloud-flink-poc"; fi
if [ ${FLINK_ENV} = "acceptance" ] | [ ${FLINK_ENV} = "acc" ]; then export OPENSHIFT_PROJECT="ecloud-flink-acceptance"; fi
if [ ${FLINK_ENV} = "production" ]| [ ${FLINK_ENV} = "prod" ]; then export OPENSHIFT_PROJECT="ecloud-flink-production"; fi

if [ -z ${OPENSHIFT_PROJECT} ]; then echo Error: Specified environment not supported ; exit 1; fi


oc project "$OPENSHIFT_PROJECT"