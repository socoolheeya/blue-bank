#!/usr/bin/env bash
set -euo pipefail

context="${KUBE_CONTEXT:-blue-bank}"
application="${ARGO_APPLICATION_NAME:-blue-bank-local}"

for _ in {1..60}; do
  sync="$(kubectl --context "$context" -n argocd get application "$application" -o jsonpath='{.status.sync.status}' 2>/dev/null || true)"
  health="$(kubectl --context "$context" -n argocd get application "$application" -o jsonpath='{.status.health.status}' 2>/dev/null || true)"
  if [[ "$sync" == "Synced" && "$health" == "Healthy" ]]; then
    ready=true
    for service in account loan card deposit; do
      available="$(kubectl --context "$context" -n blue-bank get "deployment/blue-bank-$service" -o jsonpath='{.status.conditions[?(@.type=="Available")].status}' 2>/dev/null || true)"
      [[ "$available" == "True" ]] || ready=false
    done
    if [[ "$ready" == "true" ]]; then
      echo "$application: Synced Healthy; 4 deployments Available"
      exit 0
    fi
  fi
  sleep 5
done

kubectl --context "$context" -n argocd get application "$application" -o yaml
exit 1
