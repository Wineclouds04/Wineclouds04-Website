#!/usr/bin/env bash
set -Eeuo pipefail

PUBLIC_URL="${PUBLIC_URL:-https://${PUBLIC_HOST:?Set PUBLIC_HOST or PUBLIC_URL}}"
ADMIN_URL="${ADMIN_URL:-https://${ADMIN_HOST:?Set ADMIN_HOST or ADMIN_URL}}"

retry() {
  local url="$1"
  local expected="${2:-}"
  for attempt in {1..30}; do
    body="$(curl --fail --silent --show-error --max-time 10 "$url" 2>/dev/null || true)"
    if [[ -n "$body" && ( -z "$expected" || "$body" == *"$expected"* ) ]]; then
      return 0
    fi
    sleep 2
  done
  echo "Smoke check failed: $url" >&2
  return 1
}

retry "$PUBLIC_URL/healthz" "ok"
retry "$PUBLIC_URL/api/v1/status" '"status":"ok"'
retry "$PUBLIC_URL/" "余白札记"
retry "$PUBLIC_URL/rss.xml" "<rss"
retry "$PUBLIC_URL/sitemap.xml" "<urlset"
retry "$ADMIN_URL/healthz" "ok"
retry "$ADMIN_URL/login" "PERSONAL BLOG"

echo "Production smoke tests passed."
