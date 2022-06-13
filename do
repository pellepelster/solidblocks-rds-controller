#!/usr/bin/env bash

set -eu

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

function task_cli() {
    local db_args="--db-password $(pass solidblocks/integration-test/db_password) --db-path $(pwd)/integration-test"
    "${DIR}/gradlew" solidblocks-cli:run --args="${db_args} $*"
}

function task_increment_version() {
    echo "SNAPSHOT-$(date +%Y%m%d%H%M%S)" > "${DIR}/version.txt"
}

function task_publish() {
  export GITHUB_USERNAME="pellepelster"
  export GITHUB_TOKEN="$(pass solidblocks-rds/github/personal_access_token_rw)"
  echo "${GITHUB_TOKEN}" | docker login ghcr.io -u "${GITHUB_USERNAME}" --password-stdin

  "${DIR}/gradlew" assemble publish
  #"${DIR}/gradlew" assemble docker publish dockerPush
}

function task_test() {
  task_prepare_agent_integration_test
  "${DIR}/gradlew" check
}

function task_prepare_integration_test() {
    local timestamp_now=$(date +%Y%m%d%H%M%S)

    local solidblocks_blue_version="BLUE-${timestamp_now}"
    local solidblocks_green_version="GREEN-${timestamp_now}"

    # rds postgresql
    local rds_postgresql_distributions_dir="${DIR}/solidblocks-rds-postgresql-agent/build/distributions"
    local rds_postgresql_artefacts_dir="${DIR}/solidblocks-rds-postgresql-agent/src/test/resources/rds-postgresql-agent/bootstrap/artefacts"

    mkdir -p "${rds_postgresql_artefacts_dir}"
    rm -rf ${rds_postgresql_artefacts_dir}/*.tar
    rm -rf ${rds_postgresql_artefacts_dir}/*.version

    SOLIDBLOCKS_VERSION="${solidblocks_blue_version}" "${DIR}/gradlew" solidblocks-rds-postgresql-agent:assemble
    SOLIDBLOCKS_VERSION="${solidblocks_green_version}" "${DIR}/gradlew" solidblocks-rds-postgresql-agent:assemble

    cp -v "${rds_postgresql_distributions_dir}/solidblocks-rds-postgresql-agent-${solidblocks_blue_version}.tar" "${rds_postgresql_artefacts_dir}"
    cp -v "${rds_postgresql_distributions_dir}/solidblocks-rds-postgresql-agent-${solidblocks_green_version}.tar" "${rds_postgresql_artefacts_dir}"

    echo "${solidblocks_blue_version}" > "${rds_postgresql_artefacts_dir}/blue.version"
    echo "${solidblocks_green_version}" > "${rds_postgresql_artefacts_dir}/green.version"

    # rds cloud init
    local rds_cloud_init_lib_dir="${DIR}/solidblocks-rds-cloud-init/build/libs"
    local rds_cloud_init_artefacts_dir="${DIR}/solidblocks-rds-cloud-init/src/test/resources/rds-cloud-init/bootstrap/artefacts"

    mkdir -p "${rds_cloud_init_artefacts_dir}"
    rm -rf ${rds_cloud_init_artefacts_dir}/*.tar
    rm -rf ${rds_cloud_init_artefacts_dir}/*.version

    SOLIDBLOCKS_VERSION="${solidblocks_blue_version}" "${DIR}/gradlew" solidblocks-rds-cloud-init:assemble
    SOLIDBLOCKS_VERSION="${solidblocks_green_version}" "${DIR}/gradlew" solidblocks-rds-cloud-init:assemble

    mkdir -p "${rds_cloud_init_artefacts_dir}"
    cp -v "${rds_cloud_init_lib_dir}/solidblocks-rds-cloud-init-${solidblocks_blue_version}.jar" "${rds_cloud_init_artefacts_dir}"
    cp -v "${rds_cloud_init_lib_dir}/solidblocks-rds-cloud-init-${solidblocks_green_version}.jar" "${rds_cloud_init_artefacts_dir}"

}

COMMAND="${1:-}"
shift || true

case ${COMMAND} in
  increment-version) task_increment_version "$@" ;;
  test) task_test "$@" ;;
  prepare-integration-test) task_prepare_integration_test "$@" ;;
  publish) task_publish "$@" ;;
esac
