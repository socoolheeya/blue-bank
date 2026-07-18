#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
bin_dir="$repo_root/.tools/bin"
mkdir -p "$bin_dir"

kind_version="v0.31.0"
helm_version="v3.20.2"
os="$(uname -s | tr '[:upper:]' '[:lower:]')"
case "$(uname -m)" in
  arm64|aarch64) arch="arm64" ;;
  x86_64|amd64) arch="amd64" ;;
  *) echo "unsupported architecture: $(uname -m)" >&2; exit 1 ;;
esac

if [[ ! -x "$bin_dir/kind" ]]; then
  curl -fsSL "https://kind.sigs.k8s.io/dl/${kind_version}/kind-${os}-${arch}" -o "$bin_dir/kind"
  chmod +x "$bin_dir/kind"
fi

if [[ ! -x "$bin_dir/helm" ]]; then
  archive="$(mktemp)"
  extract_dir="$(mktemp -d)"
  trap 'rm -f "$archive"; rm -rf "$extract_dir"' EXIT
  curl -fsSL "https://get.helm.sh/helm-${helm_version}-${os}-${arch}.tar.gz" -o "$archive"
  tar -xzf "$archive" -C "$extract_dir"
  cp "$extract_dir/${os}-${arch}/helm" "$bin_dir/helm"
  chmod +x "$bin_dir/helm"
fi

"$bin_dir/kind" version
"$bin_dir/helm" version --short
