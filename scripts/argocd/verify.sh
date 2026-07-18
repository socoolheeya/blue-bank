#!/usr/bin/env bash
set -euo pipefail

context="${KUBE_CONTEXT:-kind-blue-bank}"
application="${ARGO_APPLICATION_NAME:-blue-bank-local}"

for _ in {1..60}; do
  sync="$(kubectl --context "$context" -n argocd get application "$application" -o jsonpath='{.status.sync.status}' 2>/dev/null || true)"
  health="$(kubectl --context "$context" -n argocd get application "$application" -o jsonpath='{.status.health.status}' 2>/dev/null || true)"
  if [[ "$sync" == "Synced" && "$health" == "Healthy" ]]; then
    echo "$application: Synced Healthy"
    exit 0
  fi
  sleep 5
done

kubectl --context "$context" -n argocd get application "$application" -o yaml
exit 1
