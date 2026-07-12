#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

BACKUP_DIR="${DEPLOY_DIR}/backups"
UPLOAD_DIR="/srv/newblog/uploads"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"

if [[ -z "$(compose ps -q mysql)" ]]; then
	echo "未检测到已运行的 MySQL 容器，跳过部署前备份。"
	exit 0
fi

umask 077
mkdir -p "${BACKUP_DIR}"

DATABASE_BACKUP="${BACKUP_DIR}/newblog-${TIMESTAMP}.sql"
DATABASE_TEMP="${DATABASE_BACKUP}.tmp"

echo "正在导出 MySQL 数据库备份：${DATABASE_BACKUP}"
compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump --single-transaction --routines --events --triggers --no-tablespaces -u"$MYSQL_USER" "$MYSQL_DATABASE"' > "${DATABASE_TEMP}"
mv "${DATABASE_TEMP}" "${DATABASE_BACKUP}"
gzip -f "${DATABASE_BACKUP}"

if [[ -d "${UPLOAD_DIR}" ]]; then
	UPLOAD_BACKUP="${BACKUP_DIR}/uploads-${TIMESTAMP}.tar.gz"
	echo "正在归档上传目录：${UPLOAD_BACKUP}"
	tar -C "${UPLOAD_DIR}" -czf "${UPLOAD_BACKUP}" .
fi

find "${BACKUP_DIR}" -type f -mtime +14 -delete
echo "部署前备份完成。"
