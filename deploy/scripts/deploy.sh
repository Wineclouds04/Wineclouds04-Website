#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.production}"
version="${1:-}"
release_file="$ROOT_DIR/.release-version"

if [[ -z "$version" ]]; then
  echo "Usage: $0 <immutable-version>" >&2
  exit 1
fi
if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing environment file: $ENV_FILE" >&2
  exit 1
fi

previous_version=""
[[ -f "$release_file" ]] && previous_version="$(cat "$release_file")"
export APP_VERSION="$version"
compose=(docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/docker-compose.prod.yml")

if "${compose[@]}" ps -q mysql | grep -q .; then
  "$ROOT_DIR/deploy/scripts/backup-mysql.sh"
else
  echo "No existing MySQL container; skipping the pre-deploy backup."
fi
"${compose[@]}" build --pull backend web admin-static
"${compose[@]}" up -d --remove-orphans --wait

read_env() {
  local key="$1"
  sed -n "s/^${key}=//p" "$ENV_FILE" | tail -n 1
}
export PUBLIC_HOST="${PUBLIC_HOST:-$(read_env PUBLIC_HOST)}"
export ADMIN_HOST="${ADMIN_HOST:-$(read_env ADMIN_HOST)}"

if "$ROOT_DIR/deploy/scripts/smoke-test.sh"; then
  printf '%s' "$version" > "$release_file"
  echo "Deployment $version passed health and smoke checks."
  exit 0
fi

if [[ -n "$previous_version" ]]; then
  echo "Smoke checks failed; rolling application images back to $previous_version." >&2
  export APP_VERSION="$previous_version"
  "${compose[@]}" up -d --no-build backend web admin-static nginx --wait
  "$ROOT_DIR/deploy/scripts/smoke-test.sh"
else
  echo "Smoke checks failed and no previous release is recorded." >&2
fi

echo "Database migrations are never rolled back automatically; apply a forward fix if required." >&2
exit 1
