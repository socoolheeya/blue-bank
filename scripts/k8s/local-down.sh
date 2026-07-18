#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
kind_bin="$repo_root/.tools/bin/kind"

[[ -x "$kind_bin" ]] || { echo "kind is not installed under .tools/bin" >&2; exit 1; }
"$kind_bin" delete cluster --name blue-bank
