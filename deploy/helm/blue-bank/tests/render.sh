#!/usr/bin/env bash
set -euo pipefail

chart_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
helm_bin="${HELM_BIN:-helm}"
rendered_file="$(mktemp)"
trap 'rm -f "$rendered_file"' EXIT

"$helm_bin" lint "$chart_dir"
for values in values.yaml values-local.yaml values-nks.yaml; do
  "$helm_bin" template blue-bank "$chart_dir" -f "$chart_dir/$values" >"$rendered_file"
  test "$(grep -c '^kind: Deployment$' "$rendered_file")" -eq 4
  test "$(grep -c '^kind: Service$' "$rendered_file")" -eq 4
  grep -q 'runAsNonRoot: true' "$rendered_file"
  grep -q 'readinessProbe:' "$rendered_file"
  grep -q 'livenessProbe:' "$rendered_file"
  grep -q 'startupProbe:' "$rendered_file"
  grep -q 'http://blue-bank-account:8081' "$rendered_file"
done

"$helm_bin" template blue-bank "$chart_dir" -f "$chart_dir/values-nks.yaml" | grep -q 'REGISTRY_ENDPOINT'
