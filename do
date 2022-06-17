#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(
  cd "$(dirname "$0")"
  pwd -P
)"

###############################################################################
# do-script setup
###############################################################################

TEMP_DIR="${DIR}/.tmp.$$"
mkdir -p "${TEMP_DIR}"

function cleanup {
  rm -rf "${TEMP_DIR}"
}

trap cleanup EXIT

export SECRETS_DIR="${DIR}/secrets"

###############################################################################
# bootstrapping
###############################################################################
function bootstrap_solidblocks() {
  local dir="${1:-}"

  SOLIDBLOCKS_SHELL_VERSION="v0.0.41"
  SOLIDBLOCKS_SHELL_CHECKSUM="7c5cb9a80649a1eb9927ec96820f9d0c5d09afa3f25f6a0f8745b99dcbf931b7"

  if [[ ! -d "${dir}" ]]; then
    curl -L "https://github.com/pellepelster/solidblocks/releases/download/${SOLIDBLOCKS_SHELL_VERSION}/solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip" >"solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip"
    echo "${SOLIDBLOCKS_SHELL_CHECKSUM}  solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip" | sha256sum -c
    unzip -o "solidblocks-shell-${SOLIDBLOCKS_SHELL_VERSION}.zip"
  fi
}

function task_bootstrap() {
  bootstrap_solidblocks "${DIR}/solidblocks-shell"
  ensure_environment
  ensure_command "pass"
  exit 0
}

function ensure_environment() {
  source "${DIR}/solidblocks-shell/log.sh"
  source "${DIR}/solidblocks-shell/utils.sh"
  source "${DIR}/solidblocks-shell/pass.sh"
  source "${DIR}/solidblocks-shell/software.sh"

  pass_ensure_initialized

  software_set_export_path
}

###############################################################################
# secret handling
###############################################################################
function task_pass() {
  pass_wrapper $@
}

function task_usage {
  echo "Usage: $0
    "
  exit 1
}

ARG=${1:-}
shift || true

case "${ARG}" in
  bootstrap) task_bootstrap "$@" ;;
  *) ensure_environment ;;
esac

case "${ARG}" in
  pass) task_pass "$@" ;;
  *) task_usage ;;
esac
