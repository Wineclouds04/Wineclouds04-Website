#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.production}"
backup="${1:-}"
target_database="${2:-}"

if [[ -z "$backup" || -z "$target_database" ]]; then
  echo "Usage: RESTORE_CONFIRM=<database> $0 <backup.sql.gz.enc> <database>" >&2
  exit 1
fi
if [[ ! "$target_database" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "Database name may only contain letters, digits, and underscores." >&2
  exit 1
fi
if [[ ! -f "$backup" ]]; then
  echo "Backup does not exist: $backup" >&2
  exit 1
fi
if [[ "${RESTORE_CONFIRM:-}" != "$target_database" ]]; then
  echo "Set RESTORE_CONFIRM=$target_database to acknowledge the destructive restore." >&2
  exit 1
fi

read_env() {
  local key="$1"
  sed -n "s/^${key}=//p" "$ENV_FILE" | tail -n 1
}

BACKUP_ENCRYPTION_PASSWORD="${BACKUP_ENCRYPTION_PASSWORD:-$(read_env BACKUP_ENCRYPTION_PASSWORD)}"
: "${BACKUP_ENCRYPTION_PASSWORD:?Set BACKUP_ENCRYPTION_PASSWORD}"

password_file="$(mktemp)"
trap 'rm -f "$password_file"' EXIT
chmod 600 "$password_file"
printf '%s' "$BACKUP_ENCRYPTION_PASSWORD" > "$password_file"

if [[ -f "${backup}.sha256" ]]; then
  (cd "$(dirname "$backup")" && sha256sum --check "$(basename "${backup}.sha256")")
fi

compose=(docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/docker-compose.prod.yml")
"${compose[@]}" exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS `'"$target_database"'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"'

openssl enc -d -aes-256-cbc -pbkdf2 -pass "file:$password_file" -in "$backup" \
  | gzip -dc \
  | "${compose[@]}" exec -T mysql sh -c \
      'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "'"$target_database"'"'

echo "Restore completed into database: $target_database"
