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

export APP_VERSION="$version"
compose=(docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/docker-compose.prod.yml")

"$ROOT_DIR/deploy/scripts/preflight.sh" "$version"

if "${compose[@]}" ps -q mysql | grep -q .; then
  "$ROOT_DIR/deploy/scripts/backup-mysql.sh"
else
  echo "No existing MySQL container; skipping the pre-deploy backup."
fi
"${compose[@]}" build --pull backend web admin-static
"${compose[@]}" up -d --remove-orphans --wait
# Bind-mounted Nginx templates are rendered only when its entrypoint starts.
# Compose does not recreate the container when only template contents change.
"${compose[@]}" up -d --no-deps --force-recreate --wait nginx
printf '%s' "$version" > "$release_file"
echo "Deployment $version completed after Docker Compose health checks."
