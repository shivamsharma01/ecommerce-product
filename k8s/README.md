# Kubernetes manifests

Apply directly (no templating):

```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

Manifests target **Artifact Registry**  
`us-central1-docker.pkg.dev/my-project-chat-ai-487408/mcart/product:latest`  
(aligned with `cloudbuild.yaml` defaults: `_REGION=us-central1`, `_REPO=mcart`, `PROJECT_ID` in CI). Change the image host/project/repo in `deployment.yaml` if your GCP project differs.

## Probes

| Probe     | Path                          |
|-----------|-------------------------------|
| Startup   | `/health`                     |
| Liveness  | `/health`                     |
| Readiness | `/actuator/health/readiness`  |

`SERVER_PORT=8080` is set in the pod. The Service maps **80 → container 8080**.

## Service account

`deployment.yaml` uses `serviceAccountName: default`. For Firestore/Pub/Sub from GKE, use a dedicated Kubernetes SA bound to GCP via **Workload Identity**.

## Cloud Build

See [`../cloudbuild.yaml`](../cloudbuild.yaml). Set `_GKE_CLUSTER` (and optionally `_K8S_NAMESPACE`) to enable rollout. For manual builds without `COMMIT_SHA`:

```bash
gcloud builds submit --config=cloudbuild.yaml \
  --substitutions=COMMIT_SHA=$(git rev-parse --short HEAD)
```
