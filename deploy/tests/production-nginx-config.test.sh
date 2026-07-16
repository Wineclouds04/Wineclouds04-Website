#!/usr/bin/env bash
set -Eeuo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
template="$root_dir/deploy/nginx/production.conf.template"
compose_file="$root_dir/docker-compose.prod.yml"

assert_contains() {
  local file="$1"
  local expected="$2"
  grep -Fqx "$expected" "$file" || {
    echo "Expected $file to contain: $expected" >&2
    exit 1
  }
}

assert_contains "$template" '    server_name ${PUBLIC_HOST} ${PUBLIC_WWW_HOST} ${ADMIN_HOST};'
assert_contains "$template" '    server_name ${PUBLIC_HOST} ${PUBLIC_WWW_HOST};'
assert_contains "$template" '    ssl_certificate /etc/nginx/certs/public-fullchain.pem;'
assert_contains "$template" '    ssl_certificate_key /etc/nginx/certs/public-privkey.pem;'
assert_contains "$template" '    ssl_certificate /etc/nginx/certs/admin-fullchain.pem;'
assert_contains "$template" '    ssl_certificate_key /etc/nginx/certs/admin-privkey.pem;'
assert_contains "$compose_file" '      PUBLIC_WWW_HOST: ${PUBLIC_WWW_HOST:?Set PUBLIC_WWW_HOST}'
assert_contains "$compose_file" '      NGINX_ENVSUBST_FILTER: ^(PUBLIC_HOST|PUBLIC_WWW_HOST|ADMIN_HOST)$'

echo "Production Nginx host and certificate configuration is valid."
