#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
bin_dir="$repo_root/.tools/bin"
kind_bin="$bin_dir/kind"
helm_bin="$bin_dir/helm"
cluster_name="blue-bank"

"$repo_root/scripts/k8s/install-tools.sh"
docker info >/dev/null

if ! "$kind_bin" get clusters | grep -qx "$cluster_name"; then
  "$kind_bin" create cluster --config "$repo_root/deploy/kind/cluster.yaml"
fi

"$repo_root/gradlew" \
  :app:account:bootJar :app:loan:bootJar :app:card:bootJar :app:deposit:bootJar \
  --no-daemon --console=plain

for service in account loan card deposit; do
  docker build \
    -f "$repo_root/infra/docker/Dockerfile" \
    --build-arg "JAR_FILE=app/$service/build/libs/$service-0.0.1-SNAPSHOT.jar" \
    -t "blue-bank/$service:dev" \
    "$repo_root"
  "$kind_bin" load docker-image --name "$cluster_name" "blue-bank/$service:dev"
done

kubectl --context "kind-$cluster_name" create namespace blue-bank --dry-run=client -o yaml | \
  kubectl --context "kind-$cluster_name" apply -f -

"$helm_bin" upgrade --install blue-bank "$repo_root/deploy/helm/blue-bank" \
  --kube-context "kind-$cluster_name" \
  --namespace blue-bank \
  --values "$repo_root/deploy/helm/blue-bank/values-local.yaml" \
  --wait --timeout 5m

kubectl --context "kind-$cluster_name" -n blue-bank get pods,services
