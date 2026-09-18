#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if python3 -c 'import sys' >/dev/null 2>&1; then
  python3 "$REPO_ROOT/tools/fetch_sherpa_onnx.py" "$@"
elif python -c 'import sys' >/dev/null 2>&1; then
  python "$REPO_ROOT/tools/fetch_sherpa_onnx.py" "$@"
else
  echo "Python 3 is required to run fetch_sherpa_onnx.py" >&2
  exit 1
fi
