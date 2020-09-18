#!/usr/bin/env bash

export APPD_ENABLED=true
export APPD_TIER="${NAIS_NAMESPACE}_${NAIS_APP_NAME}"
export APP_NAME="sosialhjelp-innsyn-api"
export APPDYNAMICS_CONTROLLER_HOST_NAME="appdynamics.adeo.no"
export APPDYNAMICS_CONTROLLER_PORT="443"
export APPDYNAMICS_CONTROLLER_SSL_ENABLED="true"