#!/usr/bin/env bash
set -euo pipefail

context="kind-blue-bank"
namespace="blue-bank"

for item in account:8081 loan:8082 card:8083 deposit:8084; do
  service="${item%%:*}"
  port="${item##*:}"
  kubectl --context "$context" -n "$namespace" rollout status "deployment/blue-bank-$service" --timeout=3m
  kubectl --context "$context" -n "$namespace" port-forward "service/blue-bank-$service" "$port:$port" >/tmp/blue-bank-${service}-port-forward.log 2>&1 &
  forward_pid=$!
  trap 'kill "$forward_pid" 2>/dev/null || true' EXIT
  status="000"
  for _ in {1..30}; do
    status="$(curl -sS -o /dev/null -w '%{http_code}' "http://127.0.0.1:$port/" 2>/dev/null || true)"
    if [[ "$status" =~ ^[1-5][0-9][0-9]$ ]]; then
      break
    fi
    sleep 1
  done
  kill "$forward_pid" 2>/dev/null || true
  wait "$forward_pid" 2>/dev/null || true
  trap - EXIT
  [[ "$status" =~ ^[1-5][0-9][0-9]$ ]] || { cat "/tmp/blue-bank-${service}-port-forward.log"; exit 1; }
  echo "$service: HTTP $status"
done
