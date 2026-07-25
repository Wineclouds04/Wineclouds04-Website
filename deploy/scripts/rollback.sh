#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.production}"
version="${1:-}"

fail() {
  echo "Rollback failed: $*" >&2
  exit 1
}

read_env() {
  local key="$1"
  sed -n "s/^${key}=//p" "$ENV_FILE" | tail -n 1 | tr -d '\r'
}

[[ -f "$ENV_FILE" ]] || fail "missing environment file: $ENV_FILE"
[[ "$version" =~ ^[0-9]{4}\.[0-9]{2}\.[0-9]{2}\.[0-9]+$ ]] \
  || fail "version must use YYYY.MM.DD.N format"
[[ "${ROLLBACK_CONFIRM:-}" == "$version" ]] \
  || fail "set ROLLBACK_CONFIRM=$version to acknowledge the rollback"

registry="$(read_env IMAGE_REGISTRY)"
registry="${registry:-local}"
images=(
  "$registry/wineclouds04-website-backend:$version"
  "$registry/wineclouds04-website-web:$version"
  "$registry/wineclouds04-website-admin:$version"
)

for image in "${images[@]}"; do
  docker image inspect "$image" >/dev/null 2>&1 \
    || fail "required rollback image is unavailable: $image"
done

export APP_VERSION="$version"
compose=(
  docker compose
  --env-file "$ENV_FILE"
  -f "$ROOT_DIR/docker-compose.yml"
  -f "$ROOT_DIR/docker-compose.prod.yml"
)

"${compose[@]}" config --quiet
"${compose[@]}" up -d --no-build --remove-orphans --wait
printf '%s' "$version" > "$ROOT_DIR/.release-version"

echo "Application rollback to $version completed after Docker Compose health checks."
echo "Database contents and Flyway schema were not changed by this script."
