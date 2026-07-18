# Blue Bank Kubernetes and Argo CD GitOps Design

## Goal

Replace the current manual NCP VM deployment with a reproducible path that is first verified on a local Kubernetes cluster and then promoted to Naver Kubernetes Service (NKS). Keep the existing VM deployment available until the NKS workload passes smoke tests.

## Scope

The first delivery deploys the account, deposit, loan, and card Spring Boot services. The batch application, production database migration, public DNS, TLS, and zero-downtime traffic cutover remain outside this first iteration.

## Chosen approach

Use one Helm application chart for Blue Bank with environment-specific `values-local.yaml` and `values-nks.yaml` files. The chart owns the common Deployment, Service, configuration, probe, resource, and rollout templates for all four services. This avoids duplicated environment manifests while keeping one versioned release unit for services that are currently deployed together. Argo CD uses Helm only to render manifests and remains responsible for reconciliation and lifecycle.

## Architecture

- GitHub is the source repository and GitOps source of truth.
- GitHub Actions compiles and tests the Gradle multi-module project, then builds one immutable image per service.
- Local images are loaded into a kind cluster for the first deployment test.
- NCP images are pushed to Naver Container Registry (NCR) using commit-SHA tags.
- Argo CD runs in the target cluster and renders the chart with the selected environment values file, then reconciles it with automated sync, pruning, and self-healing.
- Each application runs as a Deployment behind a ClusterIP Service. Dependent services reach account through Kubernetes DNS.
- The NKS overlay exposes only the selected external entry point through an NCP-supported LoadBalancer or Ingress.

## Configuration and data

Local verification retains the existing in-memory H2 databases so Kubernetes and GitOps can be learned independently of database migration. Non-secret settings use ConfigMaps or Deployment environment variables. Credentials are never committed; NCR pull credentials and repository credentials are injected as Kubernetes Secrets or configured in Argo CD/GitHub settings.

## Delivery flow

1. Compile and test all selected Gradle modules.
2. Build deterministic container images from the repository root.
3. Lint and render the Helm chart, then install it directly once to validate the local Kubernetes resources.
4. Install Argo CD and register a declarative Application that tracks the chart with local values.
5. Change an image tag in Git and verify automatic sync and rollout.
6. Revert the Git change and verify rollback.
7. Create or connect NKS and NCR, add GitHub Actions credentials, and push images tagged with the commit SHA.
8. Bootstrap the NKS Argo CD Application and verify health and smoke endpoints before any traffic cutover.

## Reliability and security

- Add startup/readiness/liveness probes using Spring Boot Actuator health endpoints.
- Define CPU and memory requests/limits and rolling-update behavior.
- Run application containers as a non-root user and use immutable image tags in NKS.
- Restrict secrets to external configuration; documentation contains names and commands, never secret values.
- Argo CD self-heal repairs manual drift, while Git revert is the normal rollback mechanism.

## Verification

- Gradle compile and test tasks succeed.
- `helm lint` and `helm template` succeed with both values files, and Kubernetes client-side validation passes where supported.
- All local Deployments become Available and Services resolve through cluster DNS.
- Smoke requests reach each service.
- Argo CD reports `Synced` and `Healthy` after a Git change.
- A Git revert restores the previous image and Argo CD returns to `Synced` and `Healthy`.
- On NKS, pods pull from NCR, become Ready, and pass the same smoke checks.

## Constraints and assumptions

- Existing uncommitted application and test changes are preserved.
- The current NCP VM remains the fallback during this iteration.
- NCP resource creation and credentials require an authenticated NCP console or CLI session supplied by the operator.
- GitHub repository and environment settings can be configured by a repository administrator.
