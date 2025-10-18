#!/usr/bin/env bash
# Minimal wrapper that reuses the ModDevGradle harness to run builds from the repo root.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXEC="${SCRIPT_DIR}/ModDevGradle-main/gradlew"

if [[ ! -x "${EXEC}" ]]; then
  echo "ModDevGradle wrapper not found at ${EXEC}" >&2
  exit 1
fi

exec "${EXEC}" -p "${SCRIPT_DIR}" "$@"
