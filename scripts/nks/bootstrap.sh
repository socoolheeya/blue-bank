#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${KUBE_CONTEXT:?Set KUBE_CONTEXT to the NKS kubectl context}"

"$repo_root/scripts/nks/preflight.sh"
KUBE_CONTEXT="$KUBE_CONTEXT" \
ARGO_APPLICATION="$repo_root/deploy/argocd/application-nks.yaml" \
  "$repo_root/scripts/argocd/install.sh"
KUBE_CONTEXT="$KUBE_CONTEXT" ARGO_APPLICATION_NAME=blue-bank-nks \
  "$repo_root/scripts/argocd/verify.sh"

kubectl --context "$KUBE_CONTEXT" -n blue-bank get deployments,services,pods
