#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_DIR="${NEWBLOG_SOURCE_DIR:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
DEPLOY_DIR="${NEWBLOG_DEPLOY_DIR:-${SOURCE_DIR}}"
COMPOSE_FILE="${NEWBLOG_COMPOSE_FILE:-${SOURCE_DIR}/compose.prod.yaml}"
ENV_FILE="${NEWBLOG_ENV_FILE:-${DEPLOY_DIR}/.env}"
STATE_FILE="${DEPLOY_DIR}/.release-state"
DEPLOY_INTERFACE_VERSION="1"

# Compose 里的后端进程使用部署用户的数字 UID/GID，确保能写宿主机 uploads 目录。
export APP_UID="${NEWBLOG_APP_UID:-$(id -u)}"
export APP_GID="${NEWBLOG_APP_GID:-$(id -g)}"

if [[ ! -d "${SOURCE_DIR}" ]]; then
	echo "未找到服务器源码目录：${SOURCE_DIR}" >&2
	exit 1
fi

# Compose 会检查当前工作目录；统一切到源码目录，避免从 root 私有目录降权执行时失败。
cd "${SOURCE_DIR}"

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

read_env_value() {
	local key="$1"
	local line
	local value

	line="$(grep -E "^${key}=" "${ENV_FILE}" | tail -n 1 || true)"
	if [[ -z "${line}" ]]; then
		echo "生产环境文件缺少 ${key}：${ENV_FILE}" >&2
		exit 1
	fi

	value="${line#*=}"
	value="${value%$'\r'}"
	if [[ "${value}" == \"*\" && "${value}" == *\" ]]; then
		value="${value:1:${#value}-2}"
	elif [[ "${value}" == \'*\' && "${value}" == *\' ]]; then
		value="${value:1:${#value}-2}"
	fi

	if [[ -z "${value}" ]]; then
		echo "生产环境文件中的 ${key} 不能为空：${ENV_FILE}" >&2
		exit 1
	fi

	printf '%s\n' "${value}"
}

read_optional_env_value() {
	local key="$1"
	local line
	local value

	line="$(grep -E "^${key}=" "${ENV_FILE}" | tail -n 1 || true)"
	if [[ -z "${line}" ]]; then
		return 0
	fi

	value="${line#*=}"
	value="${value%$'\r'}"
	if [[ "${value}" == \"*\" && "${value}" == *\" ]]; then
		value="${value:1:${#value}-2}"
	elif [[ "${value}" == \'*\' && "${value}" == *\' ]]; then
		value="${value:1:${#value}-2}"
	fi

	printf '%s\n' "${value}"
}

validate_release_tag() {
	local release_tag="$1"
	if [[ ! "${release_tag}" =~ ^v[0-9]+(\.[0-9]+){0,2}([-.][0-9A-Za-z.-]+)?$ ]]; then
		echo "发布版本必须是以 v 开头的版本标签，例如 v1.0.0；当前值：${release_tag}" >&2
		exit 1
	fi
}

release_interface_version() {
	git -C "${SOURCE_DIR}" show "$1^{commit}:deploy/DEPLOY_INTERFACE_VERSION" 2> /dev/null \
		| tr -d '[:space:]'
}

require_compatible_release() {
	local release_tag="$1"
	local actual_version

	actual_version="$(release_interface_version "${release_tag}" || true)"
	if [[ "${actual_version}" != "${DEPLOY_INTERFACE_VERSION}" ]]; then
		echo "版本 ${release_tag} 不兼容当前服务器部署接口（需要 ${DEPLOY_INTERFACE_VERSION}，实际 ${actual_version:-缺失}）。" >&2
		exit 1
	fi
}

require_clean_source_tree() {
	if [[ -n "$(git -C "${SOURCE_DIR}" status --porcelain)" ]]; then
		echo "服务器源码仓库存在已跟踪或未跟踪的本地修改，拒绝切换或构建版本。" >&2
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
		if compose exec -T backend sh -c "test -w /data/newblog/uploads && curl --fail --silent --connect-timeout 3 --max-time 5 http://127.0.0.1:8080/actuator/health > /dev/null" \
			&& compose exec -T frontend node -e "fetch('http://127.0.0.1:3000/', { signal: AbortSignal.timeout(5000) }).then(response => process.exit(response.ok ? 0 : 1)).catch(() => process.exit(1))"; then
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
