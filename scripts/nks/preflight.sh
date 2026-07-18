#!/usr/bin/env bash
set -euo pipefail

: "${KUBE_CONTEXT:?Set KUBE_CONTEXT to the NKS kubectl context}"

case "$KUBE_CONTEXT" in
  kind-*|minikube|docker-desktop) echo "refusing local context: $KUBE_CONTEXT" >&2; exit 1 ;;
esac

current_cluster="$(kubectl config view -o jsonpath="{.contexts[?(@.name=='$KUBE_CONTEXT')].context.cluster}")"
[[ -n "$current_cluster" ]] || { echo "unknown kube context: $KUBE_CONTEXT" >&2; exit 1; }
kubectl --context "$KUBE_CONTEXT" auth can-i create deployments -n blue-bank | grep -qx yes
kubectl --context "$KUBE_CONTEXT" get nodes

values_file="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/deploy/helm/blue-bank/values-nks.yaml"
if grep -Eq 'REGISTRY_ENDPOINT|GIT_SHA' "$values_file"; then
  echo "replace REGISTRY_ENDPOINT and GIT_SHA in values-nks.yaml with NCR values" >&2
  exit 1
fi

kubectl --context "$KUBE_CONTEXT" -n blue-bank get secret ncr-pull-secret >/dev/null 2>&1 || {
  echo "missing blue-bank/ncr-pull-secret" >&2
  exit 1
}

echo "NKS preflight passed: $KUBE_CONTEXT ($current_cluster)"
