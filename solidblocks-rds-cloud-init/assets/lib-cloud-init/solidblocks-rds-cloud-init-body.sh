package_update

package_check_and_install "openjdk-17-jre-headless"
package_check_and_install "unzip"
package_check_and_install "ca-certificates"
package_check_and_install "curl"
package_check_and_install "python3-pip"

pip install jc

bootstrap_solidblocks

storage_mount "${SOLIDBLOCKS_STORAGE_DATA1_DEVICE}" "${SOLIDBLOCKS_STORAGE_DATA_DIR}"
storage_mount "${SOLIDBLOCKS_STORAGE_BACKUP1_DEVICE}" "${SOLIDBLOCKS_STORAGE_BACKUP_DIR}"

function solidblocks_agent_bootstrap() {
  cp "${SOLIDBLOCKS_DIR}/config/solidblocks-agent.service" /etc/systemd/system
  systemctl daemon-reload
  systemctl enable "solidblocks-agent"
  systemctl start "solidblocks-agent"
}

solidblocks_agent_bootstrap