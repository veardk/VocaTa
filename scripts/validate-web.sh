#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_DIR/vocata-web"

echo "==> web lint"
npx eslint .

echo "==> web type-check"
npm run type-check

echo "==> web build"
npm run build
