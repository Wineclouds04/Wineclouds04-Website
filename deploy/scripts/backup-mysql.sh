#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.production}"
COMPOSE=(docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/docker-compose.prod.yml")

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing production environment file: $ENV_FILE" >&2
  exit 1
fi

read_env() {
  local key="$1"
  sed -n "s/^${key}=//p" "$ENV_FILE" | tail -n 1
}

BACKUP_DIR="${BACKUP_DIR:-$(read_env BACKUP_DIR)}"
BACKUP_ENCRYPTION_PASSWORD="${BACKUP_ENCRYPTION_PASSWORD:-$(read_env BACKUP_ENCRYPTION_PASSWORD)}"
BACKUP_OSS_URI="${BACKUP_OSS_URI:-$(read_env BACKUP_OSS_URI)}"

: "${BACKUP_DIR:?Set BACKUP_DIR in the environment file}"
: "${BACKUP_ENCRYPTION_PASSWORD:?Set BACKUP_ENCRYPTION_PASSWORD in the environment file}"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
daily_dir="$BACKUP_DIR/daily"
weekly_dir="$BACKUP_DIR/weekly"
monthly_dir="$BACKUP_DIR/monthly"
mkdir -p "$daily_dir" "$weekly_dir" "$monthly_dir"

backup="$daily_dir/personal-blog-${timestamp}.sql.gz.enc"
password_file="$(mktemp)"
trap 'rm -f "$password_file" "${backup}.tmp"' EXIT
chmod 600 "$password_file"
printf '%s' "$BACKUP_ENCRYPTION_PASSWORD" > "$password_file"

"${COMPOSE[@]}" exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --quick --routines --triggers --events --hex-blob --set-gtid-purged=OFF "$MYSQL_DATABASE"' \
  | gzip -9 \
  | openssl enc -aes-256-cbc -pbkdf2 -salt -pass "file:$password_file" \
  > "${backup}.tmp"

openssl enc -d -aes-256-cbc -pbkdf2 -pass "file:$password_file" -in "${backup}.tmp" \
  | gzip -t
mv "${backup}.tmp" "$backup"
(
  cd "$daily_dir"
  sha256sum "$(basename "$backup")" > "$(basename "${backup}.sha256")"
)

if [[ "$(date -u +%u)" == "7" ]]; then
  cp "$backup" "$weekly_dir/"
  cp "${backup}.sha256" "$weekly_dir/"
fi
if [[ "$(date -u +%d)" == "01" ]]; then
  cp "$backup" "$monthly_dir/"
  cp "${backup}.sha256" "$monthly_dir/"
fi

find "$daily_dir" -type f -mtime +7 -delete
find "$weekly_dir" -type f -mtime +28 -delete
find "$monthly_dir" -type f -mtime +370 -delete

if [[ -n "$BACKUP_OSS_URI" ]]; then
  command -v ossutil >/dev/null || {
    echo "BACKUP_OSS_URI is set but ossutil is unavailable" >&2
    exit 1
  }
  ossutil cp "$backup" "${BACKUP_OSS_URI%/}/$(basename "$backup")"
  ossutil cp "${backup}.sha256" "${BACKUP_OSS_URI%/}/$(basename "${backup}.sha256")"
fi

echo "$backup"
