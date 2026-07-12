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
if ! git -C "${SOURCE_DIR}" rev-parse --verify --quiet "refs/tags/${RELEASE_TAG}^{commit}" > /dev/null; then
	echo "本地源码仓库不存在版本标签 ${RELEASE_TAG}，无法离线回滚。" >&2
	exit 1
fi
require_compatible_release "${RELEASE_TAG}"
PREVIOUS_RELEASE_TAG="$(current_release_tag || true)"

SITE_URL="$(read_env_value SITE_URL)"
if [[ ! "${SITE_URL}" =~ ^https:// ]]; then
	echo "SITE_URL 必须使用 HTTPS。" >&2
	exit 1
fi

export BACKEND_IMAGE="${NEWBLOG_BACKEND_IMAGE:-newblog-backend}"
export FRONTEND_IMAGE="${NEWBLOG_FRONTEND_IMAGE:-newblog-frontend}"
export IMAGE_TAG="${RELEASE_TAG}"

if ! docker image inspect "${BACKEND_IMAGE}:${RELEASE_TAG}" > /dev/null 2>&1; then
	echo "本机不存在后端镜像 ${BACKEND_IMAGE}:${RELEASE_TAG}，无法离线回滚。" >&2
	exit 1
fi

if ! docker image inspect "${FRONTEND_IMAGE}:${RELEASE_TAG}" > /dev/null 2>&1; then
	echo "本机不存在前端镜像 ${FRONTEND_IMAGE}:${RELEASE_TAG}，无法离线回滚。" >&2
	exit 1
fi

"${SCRIPT_DIR}/backup.sh"

git -C "${SOURCE_DIR}" checkout --detach "${RELEASE_TAG}"
if ! compose config --quiet; then
	echo "目标版本 ${RELEASE_TAG} 的 Compose 配置无效，未切换容器。" >&2
	if [[ -n "${PREVIOUS_RELEASE_TAG}" && "${PREVIOUS_RELEASE_TAG}" != "${RELEASE_TAG}" ]]; then
		git -C "${SOURCE_DIR}" checkout --detach "${PREVIOUS_RELEASE_TAG}"
	fi
	exit 1
fi

echo "正在切换到本地版本 ${RELEASE_TAG}..."
if ! compose up -d --remove-orphans --pull never \
	|| ! wait_for_services \
	|| ! "${SCRIPT_DIR}/verify-public.sh" "${SITE_URL}"; then
	echo "目标版本 ${RELEASE_TAG} 未通过回滚验证。" >&2
	compose ps >&2 || true
	compose logs --tail=120 backend frontend >&2 || true
	if [[ -n "${PREVIOUS_RELEASE_TAG}" && "${PREVIOUS_RELEASE_TAG}" != "${RELEASE_TAG}" ]] \
		&& docker image inspect "${BACKEND_IMAGE}:${PREVIOUS_RELEASE_TAG}" "${FRONTEND_IMAGE}:${PREVIOUS_RELEASE_TAG}" > /dev/null 2>&1; then
		echo "正在恢复回滚前版本 ${PREVIOUS_RELEASE_TAG}。" >&2
		require_compatible_release "${PREVIOUS_RELEASE_TAG}"
		git -C "${SOURCE_DIR}" checkout --detach "${PREVIOUS_RELEASE_TAG}"
		export IMAGE_TAG="${PREVIOUS_RELEASE_TAG}"
		if compose config --quiet \
			&& compose up -d --remove-orphans --pull never \
			&& wait_for_services \
			&& "${SCRIPT_DIR}/verify-public.sh" "${SITE_URL}"; then
			echo "已恢复回滚前版本 ${PREVIOUS_RELEASE_TAG}。" >&2
		else
			echo "回滚前版本 ${PREVIOUS_RELEASE_TAG} 也未能恢复，请检查容器日志。" >&2
		fi
	else
		echo "没有可用的回滚前镜像，请检查容器日志。" >&2
	fi
	exit 1
fi

write_current_release_tag "${RELEASE_TAG}"

echo "已回滚到本地版本 ${RELEASE_TAG}。数据库迁移未被逆向修改。"
