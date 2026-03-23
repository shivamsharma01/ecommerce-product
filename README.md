# Product service

Reactive REST API for products stored in **Firestore**, with an **outbox** and **Pub/Sub** (`product-events`) for downstream indexing.

## Requirements

- Java 17
- GCP project with Firestore + Pub/Sub for a full integration (optional locally)

## Run locally

```bash
./gradlew bootRun
```

Default port in `application.yml` is **8084**; in Kubernetes use **`SERVER_PORT=8080`**.

| Variable | Purpose |
|----------|---------|
| `GCP_PROJECT_ID` | GCP project |
| `SPRING_CLOUD_GCP_FIRESTORE_ENABLED` | `true` / `false` |
| `SPRING_CLOUD_GCP_PUBSUB_ENABLED` | `true` / `false` |
| `APP_SECURITY_ENABLED` | `true` to require JWT on `/api/**` |
| `APP_SECURITY_REQUIRED_SCOPE` | e.g. `product.admin` — all `/api/**` methods need `SCOPE_product.admin` in the JWT |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | When security is on (must match auth `iss` claim) |

Docker example (no GCP):

```bash
docker build -t product:local .
docker run --rm -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e SPRING_CLOUD_GCP_FIRESTORE_ENABLED=false \
  -e SPRING_CLOUD_GCP_PUBSUB_ENABLED=false \
  -e APP_SECURITY_ENABLED=false \
  product:local
```

## API and docs

- REST base: `/api/products`
- OpenAPI: `/v3/api-docs`, Swagger UI: `/swagger-ui.html` (when running)

## Build and test

```bash
./gradlew test
```

Uses mocked Firestore/Pub/Sub templates (no Docker).

## Kubernetes / CI

Manifests: **`ecomm-infra/deploy/k8s/apps/product/`**. See `cloudbuild.yaml` for image build. Workload Identity is preferred over JSON keys for GCP APIs.
