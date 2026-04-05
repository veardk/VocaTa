#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_DIR/vocata-admin"

echo "==> admin lint"
npx eslint .

echo "==> admin type-check"
npm run type-check

echo "==> admin build"
npm run build
