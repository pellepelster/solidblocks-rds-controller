#######################################
# configuration.sh                    #
#######################################

export SOLIDBLOCKS_DIR="${SOLIDBLOCKS_DIR:-/solidblocks}"
export SOLIDBLOCKS_DEVELOPMENT_MODE="${SOLIDBLOCKS_DEVELOPMENT_MODE:-0}"
export SOLIDBLOCKS_CONFIG_FILE="${SOLIDBLOCKS_DIR}/solidblocks.json"
export SOLIDBLOCKS_CERTIFICATES_DIR="${SOLIDBLOCKS_DIR}/certificates"
export SOLIDBLOCKS_GROUP="${SOLIDBLOCKS_GROUP:-solidblocks}"
export SOLIDBLOCKS_STORAGE_DATA_DIR="/storage/data"
export SOLIDBLOCKS_STORAGE_BACKUP_DIR="/storage/backup"

function bootstrap_solidblocks() {

  groupadd solidblocks

  # shellcheck disable=SC2086
  mkdir -p ${SOLIDBLOCKS_DIR}/{protected,service,download,instance,templates,config,lib,bin,certificates}
  chmod 770 ${SOLIDBLOCKS_DIR}/{protected,service,download,instance,templates,config,lib,bin,certificates}
  chgrp solidblocks ${SOLIDBLOCKS_DIR}/{protected,service,download,instance,templates,config,lib,bin,certificates}

  echo "SOLIDBLOCKS_HOSTNAME=${SOLIDBLOCKS_HOSTNAME}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_VERSION=${SOLIDBLOCKS_VERSION}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_AGENT=${SOLIDBLOCKS_AGENT}" >> "${SOLIDBLOCKS_DIR}/instance/environment"

  echo "${SOLIDBLOCKS_CLIENT_CA_PUBLIC_KEY}" | base64 -d > "${SOLIDBLOCKS_DIR}/protected/solidblocks_client_ca_public_key.crt"
  echo "${SOLIDBLOCKS_SERVER_PRIVATE_KEY}" | base64 -d > "${SOLIDBLOCKS_DIR}/protected/solidblocks_server_private_key.key"
  echo "${SOLIDBLOCKS_SERVER_PUBLIC_KEY}" | base64 -d > "${SOLIDBLOCKS_DIR}/protected/solidblocks_server_public_key.crt"

  (
      local temp_file="$(mktemp)"

      #TODO verify checksum
      curl_wrapper \
        "${REPOSITORY_BASE_ADDRESS:-https://pub-3b1d32d9ecc04ba18ced90b53fc2b338.r2.dev}/solidblocks-rds-controller/solidblocks-rds-cloud-init/${SOLIDBLOCKS_VERSION}/solidblocks-rds-cloud-init-${SOLIDBLOCKS_VERSION}-assets.jar" > "${temp_file}"
      cd "${SOLIDBLOCKS_DIR}" || exit 1
      unzip "${temp_file}"
      rm -rf "${temp_file}"
  )

  source "${SOLIDBLOCKS_DIR}/lib/storage.sh"
}
