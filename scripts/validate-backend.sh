#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-/tmp/juhao_m2repo}"

cd "$REPO_DIR/vocata-server"

echo "==> backend package baseline"
mvn -Dmaven.repo.local="$MAVEN_REPO_LOCAL" -Dmaven.test.skip=true package

echo "==> backend smoke baseline"
mvn -Dmaven.repo.local="$MAVEN_REPO_LOCAL" test
