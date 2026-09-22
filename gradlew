#!/usr/bin/env bash
set -euo pipefail

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "Gradle is not installed or not on PATH. The GitHub Action installs it via the setup-gradle step." >&2
exit 1
