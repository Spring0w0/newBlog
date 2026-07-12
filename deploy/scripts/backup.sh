#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export NEWBLOG_SOURCE_DIR="${NEWBLOG_SOURCE_DIR:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
export NEWBLOG_DEPLOY_DIR="${NEWBLOG_DEPLOY_DIR:-/srv/newblog}"
source "${SCRIPT_DIR}/common.sh"

BACKUP_DIR="${DEPLOY_DIR}/backups"
UPLOAD_DIR="${NEWBLOG_UPLOAD_DIR:-${DEPLOY_DIR}/uploads}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
DATABASE_BACKUP=""
DATABASE_TEMP=""
DATABASE_COMPRESSED=""
UPLOAD_TEMP=""
DATABASE_REQUIRED_MISSING="false"

cleanup_incomplete_backup() {
	if [[ -n "${DATABASE_TEMP}" ]]; then
		rm -f -- "${DATABASE_TEMP}"
	fi
	if [[ -n "${DATABASE_BACKUP}" ]]; then
		rm -f -- "${DATABASE_BACKUP}"
	fi
	if [[ -n "${DATABASE_COMPRESSED}" ]]; then
		rm -f -- "${DATABASE_COMPRESSED}"
	fi
	if [[ -n "${UPLOAD_TEMP}" ]]; then
		rm -f -- "${UPLOAD_TEMP}"
	fi
}

trap cleanup_incomplete_backup EXIT

umask 077
mkdir -p "${BACKUP_DIR}"

if [[ -n "$(compose ps -q mysql)" ]]; then
	DATABASE_BACKUP="${BACKUP_DIR}/newblog-${TIMESTAMP}.sql"
	DATABASE_TEMP="${DATABASE_BACKUP}.tmp"

	echo "正在导出 MySQL 数据库备份：${DATABASE_BACKUP}"
	compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump --single-transaction --routines --events --triggers --no-tablespaces -u"$MYSQL_USER" "$MYSQL_DATABASE"' > "${DATABASE_TEMP}"
	mv "${DATABASE_TEMP}" "${DATABASE_BACKUP}"
	DATABASE_COMPRESSED="${DATABASE_BACKUP}.gz"
	gzip -f "${DATABASE_BACKUP}"
	DATABASE_TEMP=""
	DATABASE_BACKUP=""
	DATABASE_COMPRESSED=""
else
	if [[ -n "$(current_release_tag || true)" ]]; then
		echo "已存在生产发布记录，但未检测到运行中的 MySQL 容器；拒绝把本次备份视为成功。" >&2
		DATABASE_REQUIRED_MISSING="true"
	else
		echo "首次部署尚无 MySQL 容器，跳过数据库导出。"
	fi
fi

if [[ -d "${UPLOAD_DIR}" ]]; then
	UPLOAD_BACKUP="${BACKUP_DIR}/uploads-${TIMESTAMP}.tar.gz"
	UPLOAD_TEMP="${UPLOAD_BACKUP}.tmp"
	echo "正在归档上传目录：${UPLOAD_BACKUP}"
	tar -C "${UPLOAD_DIR}" -czf "${UPLOAD_TEMP}" .
	mv "${UPLOAD_TEMP}" "${UPLOAD_BACKUP}"
	UPLOAD_TEMP=""
fi

find "${BACKUP_DIR}" -type f \
	\( -name 'newblog-*.sql.gz' -o -name 'uploads-*.tar.gz' \) \
	-mtime +14 -delete
find "${BACKUP_DIR}" -type f \
	\( -name 'newblog-*.sql.tmp' -o -name 'newblog-*.sql' -o -name 'uploads-*.tar.gz.tmp' \) \
	-mtime +1 -delete

if [[ "${DATABASE_REQUIRED_MISSING}" == "true" ]]; then
	exit 1
fi

echo "备份流程完成。"
