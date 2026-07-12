#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="${NEWBLOG_DEPLOY_DIR:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
COMPOSE_FILE="${DEPLOY_DIR}/compose.prod.yaml"
ENV_FILE="${DEPLOY_DIR}/.env"
STATE_FILE="${DEPLOY_DIR}/.release-state"

if [[ ! -f "${COMPOSE_FILE}" ]]; then
	echo "未找到生产 Compose 文件：${COMPOSE_FILE}" >&2
	exit 1
fi

if [[ ! -f "${ENV_FILE}" ]]; then
	echo "未找到生产环境文件：${ENV_FILE}" >&2
	exit 1
fi

compose() {
	docker compose --project-name newblog --env-file "${ENV_FILE}" --file "${COMPOSE_FILE}" "$@"
}

validate_release_tag() {
	local release_tag="$1"
	if [[ ! "${release_tag}" =~ ^v[0-9]+(\.[0-9]+){0,2}([-.][0-9A-Za-z.-]+)?$ ]]; then
		echo "发布版本必须是以 v 开头的版本标签，例如 v1.0.0；当前值：${release_tag}" >&2
		exit 1
	fi
}

current_release_tag() {
	if [[ -f "${STATE_FILE}" ]]; then
		tr -d '[:space:]' < "${STATE_FILE}"
	fi
}

write_current_release_tag() {
	printf '%s\n' "$1" > "${STATE_FILE}"
}

wait_for_services() {
	local attempts=24
	local attempt

	for ((attempt = 1; attempt <= attempts; attempt++)); do
		if compose exec -T backend curl --fail --silent http://127.0.0.1:8080/actuator/health > /dev/null \
			&& compose exec -T frontend node -e "fetch('http://127.0.0.1:3000/').then(response => process.exit(response.ok ? 0 : 1)).catch(() => process.exit(1))"; then
			echo "容器健康检查通过。"
			return 0
		fi

		echo "等待容器就绪（${attempt}/${attempts}）..."
		sleep 5
	done

	echo "容器在 120 秒内未通过健康检查。" >&2
	compose ps >&2 || true
	compose logs --tail=120 backend frontend >&2 || true
	return 1
}
