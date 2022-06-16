#!/usr/bin/env bash

set -eu

DIR="$( cd "$(dirname "$0")" ; pwd -P )"

SOLIDBLOCKS_DIR="${SOLIDBLOCKS_DIR:-/solidblocks}"
SOLIDBLOCKS_BOOTSTRAP_ADDRESS="${SOLIDBLOCKS_BOOTSTRAP_ADDRESS:-https://maven.pkg.github.com}"

echo "bootstrapping '${SOLIDBLOCKS_AGENT}' from '${SOLIDBLOCKS_BOOTSTRAP_ADDRESS}'"

COMPONENT_ACTIVE="${SOLIDBLOCKS_DIR}/service/${SOLIDBLOCKS_AGENT}-active"
DOWNLOAD_DIR="${SOLIDBLOCKS_DIR}/download"

if [[ -f "${COMPONENT_ACTIVE}/update.version" ]]; then
  COMPONENT_VERSION="$(cat "${COMPONENT_ACTIVE}/update.version")"
  echo "updating component to '${COMPONENT_VERSION}'"
else
  COMPONENT_VERSION="${SOLIDBLOCKS_VERSION}"
  echo "downloading initial version '${COMPONENT_VERSION}'"
fi

COMPONENT_URL="${SOLIDBLOCKS_BOOTSTRAP_ADDRESS}/${GITHUB_USERNAME}/solidblocks-rds/solidblocks-rds/${SOLIDBLOCKS_AGENT}/${COMPONENT_VERSION}/${SOLIDBLOCKS_AGENT}-${COMPONENT_VERSION}.tar"
COMPONENT_NAME="${SOLIDBLOCKS_AGENT}-${COMPONENT_VERSION}"
COMPONENT_DISTRIBUTION="${DOWNLOAD_DIR}/${COMPONENT_NAME}.tar"

DOWNLOAD_OPTIONS=""
SKIP_DOWNLOAD=0

if [ -L "${COMPONENT_ACTIVE}" ] && [ -e "${COMPONENT_ACTIVE}" ]; then
  if curl -u "${GITHUB_USERNAME}:${GITHUB_PAT}" --silent --location --show-error --fail -o /dev/null "${COMPONENT_URL}"; then
    echo "download url '${COMPONENT_URL}' exists and active component found, trying download with fallback to active version"
   DOWNLOAD_OPTIONS="--fail"
  else
    echo "download url '${COMPONENT_URL}' does not exist, falling back to active component"
    SKIP_DOWNLOAD=1
  fi
fi

echo curl -u "${GITHUB_USERNAME}:${GITHUB_PAT}" ${DOWNLOAD_OPTIONS} --retry 25 --retry-connrefused --location --show-error "${COMPONENT_URL}"

if [[ ${SKIP_DOWNLOAD} -eq 0 ]]; then
  echo "downloading '${COMPONENT_URL}' to '${COMPONENT_DISTRIBUTION}'"
  echo "------------------------------------------------------------------------------------------"
  while ! curl -u "${GITHUB_USERNAME}:${GITHUB_PAT}" ${DOWNLOAD_OPTIONS} --retry 25 --retry-connrefused --location --show-error "${COMPONENT_URL}" > "${COMPONENT_DISTRIBUTION}"; do
      echo "download failed, retrying"
      sleep 10
  done
  echo "------------------------------------------------------------------------------------------"
  printf "\n\n"

  mkdir -p "${SOLIDBLOCKS_DIR}/service"

  (
    cd "${SOLIDBLOCKS_DIR}/service"
    echo "extracting '${COMPONENT_DISTRIBUTION}' to '$(pwd)'"
    echo "------------------------------------------------------------------------------------------"
    tar -xvf "${COMPONENT_DISTRIBUTION}"
    echo "------------------------------------------------------------------------------------------"
    printf "\n\n"
  )

  rm -f "${COMPONENT_ACTIVE}"
  ln -s "${COMPONENT_NAME}" "${COMPONENT_ACTIVE}"

fi

cd "${COMPONENT_ACTIVE}"
exec "${COMPONENT_ACTIVE}/bin/${SOLIDBLOCKS_AGENT}"