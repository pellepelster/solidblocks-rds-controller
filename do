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

function task_prepare_agent_integration_test() {
    local timestamp_now=$(date +%Y%m%d%H%M%S)

    local solidblocks_blue_version="BLUE-${timestamp_now}"
    local solidblocks_green_version="GREEN-${timestamp_now}"

    local distributions_dir="${DIR}/solidblocks-rds-agent/build/distributions"
    local artefacts_dir="${DIR}/solidblocks-rds-agent/src/test/resources/rds-agent/bootstrap/artefacts"

    mkdir -p "${artefacts_dir}"
    rm -rf ${artefacts_dir}/*.tar
    rm -rf ${artefacts_dir}/*.version

    SOLIDBLOCKS_VERSION="${solidblocks_blue_version}" "${DIR}/gradlew" solidblocks-rds-agent:assemble
    SOLIDBLOCKS_VERSION="${solidblocks_green_version}" "${DIR}/gradlew" solidblocks-rds-agent:assemble

    cp "${distributions_dir}/solidblocks-rds-agent-${solidblocks_blue_version}.tar" "${artefacts_dir}/solidblocks-rds-agent-${solidblocks_blue_version}.tar"
    cp "${distributions_dir}/solidblocks-rds-agent-${solidblocks_green_version}.tar" "${artefacts_dir}/solidblocks-rds-agent-${solidblocks_green_version}.tar"

    echo "${solidblocks_blue_version}" > "${artefacts_dir}/blue.version"
    echo "${solidblocks_green_version}" > "${artefacts_dir}/green.version"
}

COMMAND="${1:-}"
shift || true

case ${COMMAND} in
  increment-version) task_increment_version "$@" ;;
  prepare-agent-integration-test) task_prepare_agent_integration_test "$@" ;;
  publish) task_publish "$@" ;;
esac
