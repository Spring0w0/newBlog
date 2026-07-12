#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
	echo "用法：$0 <site-url>" >&2
	exit 1
fi

SITE_URL="${1%/}"

if [[ ! "${SITE_URL}" =~ ^https:// ]]; then
	echo "公开站点地址必须使用 HTTPS：${SITE_URL}" >&2
	exit 1
fi

for path in / /api/site/config /rss.xml /sitemap.xml; do
	echo "检查公开地址：${SITE_URL}${path}"
	curl --fail --show-error --silent --location \
		--connect-timeout 5 --max-time 15 \
		--retry 5 --retry-all-errors --retry-delay 3 \
		"${SITE_URL}${path}" > /dev/null
done

echo "公开站点健康检查通过。"
