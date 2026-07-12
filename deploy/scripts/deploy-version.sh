#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
	echo "用法：$0 <release-tag>" >&2
	exit 1
fi

RELEASE_TAG="$1"
if [[ ! "${RELEASE_TAG}" =~ ^v[0-9]+(\.[0-9]+){0,2}([-.][0-9A-Za-z.-]+)?$ ]]; then
	echo "发布版本必须是以 v 开头的版本标签，例如 v1.0.1；当前值：${RELEASE_TAG}" >&2
	exit 1
fi

DEPLOY_DIR="${NEWBLOG_DEPLOY_DIR:-/srv/newblog}"
SOURCE_DIR="${NEWBLOG_SOURCE_DIR:-${DEPLOY_DIR}/source}"
DEPLOY_INTERFACE_VERSION="1"

if [[ ! -d "${SOURCE_DIR}/.git" ]]; then
	echo "未找到服务器源码仓库：${SOURCE_DIR}" >&2
	exit 1
fi

if [[ -n "$(git -C "${SOURCE_DIR}" status --porcelain)" ]]; then
	echo "服务器源码仓库存在已跟踪或未跟踪的本地修改，拒绝切换版本。" >&2
	exit 1
fi

echo "正在获取版本 ${RELEASE_TAG} 的源码..."
git -C "${SOURCE_DIR}" fetch --prune --tags origin

if ! git -C "${SOURCE_DIR}" rev-parse --verify --quiet "refs/tags/${RELEASE_TAG}^{commit}" > /dev/null; then
	echo "远程仓库不存在版本标签 ${RELEASE_TAG}。" >&2
	exit 1
fi

git -C "${SOURCE_DIR}" fetch origin master
if ! git -C "${SOURCE_DIR}" merge-base --is-ancestor "${RELEASE_TAG}^{commit}" origin/master; then
	echo "版本 ${RELEASE_TAG} 不属于 origin/master，拒绝部署。" >&2
	exit 1
fi

TARGET_INTERFACE_VERSION="$(
	git -C "${SOURCE_DIR}" show "${RELEASE_TAG}^{commit}:deploy/DEPLOY_INTERFACE_VERSION" 2> /dev/null \
		| tr -d '[:space:]' \
		|| true
)"
if [[ "${TARGET_INTERFACE_VERSION}" != "${DEPLOY_INTERFACE_VERSION}" ]]; then
	echo "版本 ${RELEASE_TAG} 不兼容当前服务器部署接口（需要 ${DEPLOY_INTERFACE_VERSION}，实际 ${TARGET_INTERFACE_VERSION:-缺失}）。" >&2
	exit 1
fi

git -C "${SOURCE_DIR}" checkout --detach "${RELEASE_TAG}"

if [[ ! -x "${SOURCE_DIR}/deploy/scripts/deploy.sh" ]]; then
	echo "版本 ${RELEASE_TAG} 的部署脚本不可执行，拒绝部署。" >&2
	exit 1
fi

exec env \
	NEWBLOG_DEPLOY_DIR="${DEPLOY_DIR}" \
	NEWBLOG_SOURCE_DIR="${SOURCE_DIR}" \
	"${SOURCE_DIR}/deploy/scripts/deploy.sh" "${RELEASE_TAG}"
