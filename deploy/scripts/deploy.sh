#!/usr/bin/env bash

set -Eeuo pipefail

if [[ -z "${RELEASE_TAG:-}" ]]; then
	echo "必须通过 RELEASE_TAG 环境变量指定发布版本。" >&2
	exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

validate_release_tag "${RELEASE_TAG}"
PREVIOUS_RELEASE_TAG="$(current_release_tag || true)"

if [[ "${PREVIOUS_RELEASE_TAG}" == "${RELEASE_TAG}" ]]; then
	echo "版本 ${RELEASE_TAG} 已处于部署状态，跳过重复发布。"
	exit 0
fi

"${SCRIPT_DIR}/backup.sh"

echo "正在拉取版本 ${RELEASE_TAG} 的应用镜像..."
export IMAGE_TAG="${RELEASE_TAG}"
compose pull backend frontend

echo "正在启动版本 ${RELEASE_TAG}..."
if compose up -d --remove-orphans && wait_for_services; then
	write_current_release_tag "${RELEASE_TAG}"
	echo "版本 ${RELEASE_TAG} 已部署完成。"
	exit 0
fi

echo "版本 ${RELEASE_TAG} 部署失败。" >&2
if [[ -n "${PREVIOUS_RELEASE_TAG}" ]]; then
	echo "尝试自动回滚到上一版本 ${PREVIOUS_RELEASE_TAG}..." >&2
	"${SCRIPT_DIR}/rollback.sh" "${PREVIOUS_RELEASE_TAG}"
else
	echo "没有可自动回滚的上一版本；请查看容器日志和部署前备份。" >&2
fi

exit 1
