#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
	echo "用法：$0 <release-tag>" >&2
	exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

RELEASE_TAG="$1"
validate_release_tag "${RELEASE_TAG}"

echo "正在回滚到版本 ${RELEASE_TAG}..."
export IMAGE_TAG="${RELEASE_TAG}"
compose pull backend frontend
compose up -d --remove-orphans
wait_for_services
write_current_release_tag "${RELEASE_TAG}"

echo "已回滚到版本 ${RELEASE_TAG}。"
