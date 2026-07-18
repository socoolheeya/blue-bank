#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
context="${KUBE_CONTEXT:-blue-bank}"
argo_version="v3.4.2"

kubectl --context "$context" create namespace argocd --dry-run=client -o yaml | kubectl --context "$context" apply -f -
kubectl --context "$context" apply -n argocd --server-side --force-conflicts \
  -f "https://raw.githubusercontent.com/argoproj/argo-cd/${argo_version}/manifests/install.yaml"
kubectl --context "$context" apply -f "$repo_root/deploy/argocd/config.yaml"
kubectl --context "$context" -n argocd rollout restart statefulset/argocd-application-controller
kubectl --context "$context" -n argocd rollout status deployment/argocd-server --timeout=5m
kubectl --context "$context" -n argocd rollout status statefulset/argocd-application-controller --timeout=5m
kubectl --context "$context" apply -f "$repo_root/deploy/argocd/project.yaml"
kubectl --context "$context" apply -f "${ARGO_APPLICATION:-$repo_root/deploy/argocd/application-local.yaml}"
