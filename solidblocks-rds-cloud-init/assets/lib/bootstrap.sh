#######################################
# configuration.sh                    #
#######################################

export SOLIDBLOCKS_DIR="${SOLIDBLOCKS_DIR:-/solidblocks}"
export SOLIDBLOCKS_DEVELOPMENT_MODE="${SOLIDBLOCKS_DEVELOPMENT_MODE:-0}"
export SOLIDBLOCKS_CONFIG_FILE="${SOLIDBLOCKS_DIR}/solidblocks.json"
export SOLIDBLOCKS_CERTIFICATES_DIR="${SOLIDBLOCKS_DIR}/certificates"
export SOLIDBLOCKS_GROUP="${SOLIDBLOCKS_GROUP:-solidblocks}"
export SOLIDBLOCKS_STORAGE_LOCAL_DIR="/storage/local"

function bootstrap_solidblocks() {

  groupadd solidblocks

  # shellcheck disable=SC2086
  mkdir -p ${SOLIDBLOCKS_DIR}/{protected,service,download,instance,templates,config,lib,bin,certificates}
  chmod 770 ${SOLIDBLOCKS_DIR}/{protected,service,download,instance,templates,config,lib,bin,certificates}
  chgrp solidblocks ${SOLIDBLOCKS_DIR}/{protected,service,download,instance,templates,config,lib,bin,certificates}

  echo "SOLIDBLOCKS_HOSTNAME=${SOLIDBLOCKS_HOSTNAME}" >> "${SOLIDBLOCKS_DIR}/instance/environment"
  echo "SOLIDBLOCKS_VERSION=${SOLIDBLOCKS_VERSION}" >> "${SOLIDBLOCKS_DIR}/instance/environment"

  (
      local temp_file="$(mktemp)"

      #TODO verify checksum
      # solidblocks-rds-cloud-init-SNAPSHOT-20220613183343-assets.jar
      # https://maven.pkg.github.com/pellepelster/solidblocks-rds/solidblocks-rds/solidblocks-rds-cloud-init/SNAPSHOT-20220613183343/solidblocks-rds-cloud-init-SNAPSHOT-20220613183343-assets.jar

      curl_wrapper -L \
        "${REPOSITORY_BASE_ADDRESS:-https://maven.pkg.github.com}/${GITHUB_USERNAME}/solidblocks-rds/solidblocks-rds/solidblocks-rds-cloud-init/${SOLIDBLOCKS_VERSION}/solidblocks-rds-cloud-init-${SOLIDBLOCKS_VERSION}-assets.jar" > "${temp_file}"
      cd "${SOLIDBLOCKS_DIR}" || exit 1
      unzip "${temp_file}"
      rm -rf "${temp_file}"
  )
}
