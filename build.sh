#!/bin/bash
# Denne filen må ha LF som line separator.

# Stop scriptet om en kommando feiler
set -e

# Usage string
usage="Script som bygger prosjektet og publiserer til nexus

Om environment variabelen 'versjon' er satt vil den erstatte versjonen som ligger i pom.xml.

Bruk:
./$(basename "$0") OPTIONS

Gyldige OPTIONS:
    -h  | --help        - printer denne hjelpeteksten
"

# Default verdier
PROJECT_ROOT="$( cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Hent ut argumenter
for arg in "$@"
do
case $arg in
    -h|--help)
    echo "$usage" >&2
    exit 1
    ;;
    *) # ukjent argument
    printf "Ukjent argument: %s\n" "$1" >&2
    echo ""
    echo "$usage" >&2
    exit 1
    ;;
esac
done

function go_to_project_root() {
    cd ${PROJECT_ROOT}
}

function export_version() {
    GIT_COMMIT_HASH=$(git log -n 1 --pretty=format:'%h')
    GIT_COMMIT_DATE=$(git log -1 --pretty='%ad' --date=format:'%Y%m%d.%H%M')
    export versjon="1.0_${GIT_COMMIT_DATE}_${GIT_COMMIT_HASH}"
}

function docker_login() {
    echo ${REPO_PASSWORD} | docker login -u ${REPO_USERNAME} --password-stdin repo.adeo.no:5443
}

function build_and_deploy_docker() {
    docker build . -t repo.adeo.no:5443/sosialhjelp-innsyn-api:${versjon} -f Dockerfile.jenkins
    docker push repo.adeo.no:5443/sosialhjelp-innsyn-api:${versjon}
}


go_to_project_root
export_version
docker_login
build_and_deploy_docker
