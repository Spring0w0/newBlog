#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
	echo "用法：$0 <release-tag>" >&2
	exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export NEWBLOG_SOURCE_DIR="${NEWBLOG_SOURCE_DIR:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
export NEWBLOG_DEPLOY_DIR="${NEWBLOG_DEPLOY_DIR:-/srv/newblog}"
source "${SCRIPT_DIR}/common.sh"

RELEASE_TAG="$1"
validate_release_tag "${RELEASE_TAG}"
require_clean_source_tree
require_compatible_release "${RELEASE_TAG}"
PREVIOUS_RELEASE_TAG="$(current_release_tag || true)"

if git -C "${SOURCE_DIR}" rev-parse --is-inside-work-tree > /dev/null 2>&1; then
	EXPECTED_COMMIT="$(git -C "${SOURCE_DIR}" rev-list -n 1 "${RELEASE_TAG}")"
	CURRENT_COMMIT="$(git -C "${SOURCE_DIR}" rev-parse HEAD)"
	if [[ -z "${EXPECTED_COMMIT}" || "${CURRENT_COMMIT}" != "${EXPECTED_COMMIT}" ]]; then
		echo "源码目录没有检出版本 ${RELEASE_TAG}，拒绝构建。" >&2
		exit 1
	fi
fi

SITE_URL="$(read_env_value SITE_URL)"
NEXT_PUBLIC_API_BASE_URL="$(read_env_value NEXT_PUBLIC_API_BASE_URL)"
BUILD_PROXY_URL="$(read_optional_env_value BUILD_PROXY_URL)"
if [[ ! "${SITE_URL}" =~ ^https:// || ! "${NEXT_PUBLIC_API_BASE_URL}" =~ ^https:// ]]; then
	echo "SITE_URL 和 NEXT_PUBLIC_API_BASE_URL 必须使用 HTTPS。" >&2
	exit 1
fi

FRONTEND_BUILD_NETWORK_ARGS=()
FRONTEND_BUILD_PROXY_ARGS=()
if [[ -n "${BUILD_PROXY_URL}" ]]; then
	BUILD_PROXY_URL="${BUILD_PROXY_URL%/}"
	if [[ ! "${BUILD_PROXY_URL}" =~ ^http://127\.0\.0\.1:([0-9]{1,5})$ ]]; then
		echo "BUILD_PROXY_URL 只允许使用本机 HTTP 代理，例如 http://127.0.0.1:7890。" >&2
		exit 1
	fi
	BUILD_PROXY_PORT="${BASH_REMATCH[1]}"
	if ((10#${BUILD_PROXY_PORT} < 1 || 10#${BUILD_PROXY_PORT} > 65535)); then
		echo "BUILD_PROXY_URL 的端口必须在 1 到 65535 之间。" >&2
		exit 1
	fi

	if ! curl --proxy "${BUILD_PROXY_URL}" --connect-timeout 5 --max-time 30 \
		--fail --silent --show-error --output /dev/null https://registry.npmmirror.com/; then
		echo "BUILD_PROXY_URL 无法访问前端依赖镜像站，停止构建。" >&2
		exit 1
	fi

	echo "前端构建依赖下载将使用本机代理 ${BUILD_PROXY_URL}。"
	FRONTEND_BUILD_NETWORK_ARGS=(--network host)
	FRONTEND_BUILD_PROXY_ARGS=(
		--build-arg "HTTP_PROXY=${BUILD_PROXY_URL}"
		--build-arg "HTTPS_PROXY=${BUILD_PROXY_URL}"
		--build-arg "http_proxy=${BUILD_PROXY_URL}"
		--build-arg "https_proxy=${BUILD_PROXY_URL}"
		--build-arg "NO_PROXY=localhost,127.0.0.1,::1"
		--build-arg "no_proxy=localhost,127.0.0.1,::1"
	)
fi

export BACKEND_IMAGE="${NEWBLOG_BACKEND_IMAGE:-newblog-backend}"
export FRONTEND_IMAGE="${NEWBLOG_FRONTEND_IMAGE:-newblog-frontend}"
export IMAGE_TAG="${RELEASE_TAG}"

compose config --quiet

AVAILABLE_KB="$(df -Pk "${DEPLOY_DIR}" | awk 'NR == 2 { print $4 }')"
MINIMUM_KB=$((8 * 1024 * 1024))
if [[ ! "${AVAILABLE_KB}" =~ ^[0-9]+$ || "${AVAILABLE_KB}" -lt "${MINIMUM_KB}" ]]; then
	echo "部署目录所在文件系统可用空间不足 8GB，停止构建；不会自动删除镜像、备份或数据卷。" >&2
	exit 1
fi

if [[ -n "${PREVIOUS_RELEASE_TAG}" && -z "$(compose ps -q mysql)" ]]; then
	echo "已记录在用版本 ${PREVIOUS_RELEASE_TAG}，但 MySQL 未运行；为避免无数据库备份升级，停止部署。" >&2
	exit 1
fi

if ! docker image inspect mysql:8.4 > /dev/null 2>&1; then
	echo "本机尚无 mysql:8.4，正在通过已配置的 Docker Hub 镜像加速下载..."
	docker pull mysql:8.4
fi

echo "正在顺序构建后端版本 ${RELEASE_TAG}..."
docker build \
	--file "${SOURCE_DIR}/backend/Dockerfile" \
	--build-arg "MAVEN_OPTS=-Xmx512m -XX:MaxMetaspaceSize=256m" \
	--label "org.opencontainers.image.revision=${CURRENT_COMMIT:-unknown}" \
	--label "org.opencontainers.image.version=${RELEASE_TAG}" \
	--tag "${BACKEND_IMAGE}:${RELEASE_TAG}" \
	"${SOURCE_DIR}/backend"

echo "后端构建完成，开始构建前端版本 ${RELEASE_TAG}..."
docker build \
	"${FRONTEND_BUILD_NETWORK_ARGS[@]}" \
	"${FRONTEND_BUILD_PROXY_ARGS[@]}" \
	--file "${SOURCE_DIR}/frontend/Dockerfile" \
	--build-arg "NODE_OPTIONS=--max-old-space-size=768" \
	--build-arg "NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}" \
	--build-arg "SITE_URL=${SITE_URL}" \
	--build-arg "INTERNAL_API_BASE_URL=http://127.0.0.1:8080" \
	--label "org.opencontainers.image.revision=${CURRENT_COMMIT:-unknown}" \
	--label "org.opencontainers.image.version=${RELEASE_TAG}" \
	--tag "${FRONTEND_IMAGE}:${RELEASE_TAG}" \
	"${SOURCE_DIR}/frontend"

docker image inspect "${BACKEND_IMAGE}:${RELEASE_TAG}" "${FRONTEND_IMAGE}:${RELEASE_TAG}" > /dev/null

"${SCRIPT_DIR}/backup.sh"

echo "正在启动本地构建的版本 ${RELEASE_TAG}..."
if ! compose up -d --remove-orphans --pull never \
	|| ! wait_for_services \
	|| ! "${SCRIPT_DIR}/verify-public.sh" "${SITE_URL}"; then
	echo "版本 ${RELEASE_TAG} 未通过部署验证。现有数据卷不会被删除。" >&2
	compose ps >&2 || true
	compose logs --tail=120 backend frontend >&2 || true
	if [[ -n "${PREVIOUS_RELEASE_TAG}" && "${PREVIOUS_RELEASE_TAG}" != "${RELEASE_TAG}" ]] \
		&& docker image inspect "${BACKEND_IMAGE}:${PREVIOUS_RELEASE_TAG}" "${FRONTEND_IMAGE}:${PREVIOUS_RELEASE_TAG}" > /dev/null 2>&1; then
		echo "正在恢复上一版本地应用镜像 ${PREVIOUS_RELEASE_TAG}；数据库迁移不会被逆向修改。" >&2
		require_compatible_release "${PREVIOUS_RELEASE_TAG}"
		git -C "${SOURCE_DIR}" checkout --detach "${PREVIOUS_RELEASE_TAG}"
		export IMAGE_TAG="${PREVIOUS_RELEASE_TAG}"
		if compose config --quiet \
			&& compose up -d --remove-orphans --pull never \
			&& wait_for_services \
			&& "${SCRIPT_DIR}/verify-public.sh" "${SITE_URL}"; then
			echo "已恢复上一版本 ${PREVIOUS_RELEASE_TAG}。" >&2
		else
			echo "上一版本 ${PREVIOUS_RELEASE_TAG} 也未能恢复，请检查容器日志。" >&2
		fi
	else
		echo "没有可用的上一版本地镜像，请检查容器日志。" >&2
	fi
	exit 1
fi

write_current_release_tag "${RELEASE_TAG}"

echo "版本 ${RELEASE_TAG} 已部署完成。"
