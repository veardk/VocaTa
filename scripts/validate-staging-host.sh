#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_DIR="${APP_DIR:-$(cd "$REPO_DIR/.." && pwd)}"
ENV_FILE="${ENV_FILE:-$APP_DIR/.env}"

cd "$REPO_DIR"

echo "==> staging compose status"
docker compose --env-file "$ENV_FILE" ps

echo "==> staging backend health"
curl -fsS http://127.0.0.1:9009/api/health

echo "==> staging character list smoke"
curl -fsS "http://127.0.0.1:9009/api/open/character/list?pageNum=1&pageSize=2"
