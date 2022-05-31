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

COMMAND="${1:-}"
shift || true

case ${COMMAND} in
  increment-version) task_increment_version "$@" ;;
  publish) task_publish "$@" ;;
esac
