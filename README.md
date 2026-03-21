# Product Service

Spring Boot **WebFlux** microservice for product CRUD backed by **Google Cloud Firestore**, with an **outbox** table and **Pub/Sub** publishing for downstream search/indexing. Uses **reactive Firestore transactions** so product documents and outbox rows commit together, and optional **JWT** protection for HTTP APIs.

## Requirements

- **Java 17**
- **Gradle** (wrapper included)
- For GCP integration: Firestore + Pub/Sub in a project, and a service account JSON with `roles/datastore.user` and `roles/pubsub.publisher`

## Quick start

```bash
./gradlew bootRun
```

Default port: **8084** (`server.port` in `application.yml`).

Run tests (Firestore/Pub/Sub mocked, security open):

```bash
./gradlew test
```

## Container image (Docker)

Multi-stage **`Dockerfile`**: Gradle `bootJar` (layered) → JRE image, non-root user `spring` (uid **1001**), **`SERVER_PORT=8080`**, `JarLauncher` for fast layer reuse.

```bash
docker build -t product:local .
docker run --rm -p 8080:8080 \
  -e SPRING_CLOUD_GCP_FIRESTORE_ENABLED=false \
  -e SPRING_CLOUD_GCP_PUBSUB_ENABLED=false \
  -e APP_SECURITY_ENABLED=false \
  product:local
```

`server.port` defaults to **8084** locally; override with **`SERVER_PORT`** (set to **8080** in Kubernetes).

## Kubernetes & Cloud Build

- **`k8s/deployment.yaml`** — **product** deployment (2 replicas, probes, optional `product-config` / `product-secrets`); image **`us-central1-docker.pkg.dev/my-project-chat-ai-487408/mcart/product:latest`** — change project/repo/region if needed.
- **`k8s/service.yaml`** — **ClusterIP** Service **product**, port **80** → **http** (8080).
- **`cloudbuild.yaml`** — Docker build, Artifact Registry tags, optional GKE rollout when `_GKE_CLUSTER` is set.

See [`k8s/README.md`](k8s/README.md) for `kubectl apply` and probes.

## HTTP API

Base path: `/api/products`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/products` | Create product (server-generated string `id`) |
| `GET` | `/api/products` | List all products |
| `GET` | `/api/products/{id}` | Get by id |
| `PUT` | `/api/products/{id}` | Full update |
| `DELETE` | `/api/products/{id}` | Delete |

**Errors:** Validation **400**; not found **404**; duplicate SKU **409**; outbox persistence failure **503** (retry-safe). When JWT security is enabled, missing/invalid token **401**; missing scope **403** (JSON bodies for auth errors).

Product IDs are strings (e.g. `P` + 12 hex chars). Each product has a monotonic **`version`** used for event ordering and downstream conflict handling.

## Configuration

### GCP (`application.yml` or env)

| Property | Purpose |
|----------|---------|
| `spring.cloud.gcp.project-id` | GCP project |
| `spring.cloud.gcp.credentials.location` | Service account JSON path (prefer env-specific config, not committed secrets) |
| `spring.cloud.gcp.firestore.enabled` | Set `false` for tests or Firestore-less runs |
| `spring.cloud.gcp.pubsub.enabled` | Set `false` when Pub/Sub is not needed |

### Security (`app.security.*`)

| Property | Purpose |
|----------|---------|
| `app.security.enabled` | `true` = JWT required for `/api/**` (default in repo is `false` for local dev) |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | Auth-server issuer (or use `jwk-set-uri`) |
| `app.security.cors-allowed-origins` | List of allowed browser origins (CORS) |
| `app.security.required-scope` | If set, writes require `SCOPE_<value>`; reads only need authentication |

### Example (production-style)

```yaml
app:
  security:
    enabled: true
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI}
# app.security.cors-allowed-origins, required-scope as needed
```

### OpenAPI (user-service style)

- **Spec:** [`/v3/api-docs`](http://localhost:8084/v3/api-docs) (JSON)
- **UI:** [`/swagger-ui.html`](http://localhost:8084/swagger-ui.html)
- Product endpoints declare **`@SecurityRequirement(name = "bearer-jwt")`**; the scheme is registered in `OpenApiConfig` as HTTP **Bearer JWT** (same name as typical user services).
- With **`app.security.enabled=true`**, `/api/**` still **enforces** JWT at runtime; Swagger UI and `/v3/api-docs/**` stay **permitAll** so you can open docs and use **Authorize** with a token.

## Architecture

```
┌─────────────────────┐
│  Product Service    │
│  REST → Service     │
│  Firestore TX       │
│  (products + outbox)│
└──────────┬──────────┘
           │ scheduled poll
           ▼
   Pub/Sub topic: product-events
           │
           ▼
   Consumers (e.g. indexer → Elasticsearch)
```

- **Writes:** `ProductService` runs under `@Transactional(transactionManager = "firestoreTransactionManager")` so **product** and **outbox** documents are committed in **one Firestore transaction**.
- **Publish:** `OutboxPublisherJob` loads **PENDING** outbox rows (indexed query), publishes to Pub/Sub, waits for publish completion, then marks **SENT**.

## Firestore

Collections are created on first use:

| Collection | Role |
|------------|------|
| `products` | Product documents (`productId`, `sku`, `version`, …) |
| `outbox_events` | Outbox rows (`status`, `payload`, `createdAt`, …) |

### Indexes

- **`products`:** single-field equality on `sku` (automatic).
- **`outbox_events`:** composite **`status` + `createdAt`** for pending queries.

Deploy indexes with Firebase CLI using [`firestore.indexes.json`](firestore.indexes.json), or create the composite index when the console / error URL prompts you.

Firestore transactions require **all reads before any writes** in the same transaction; the service ordering satisfies `FirestoreTemplate`’s rules.

## Pub/Sub

| Resource | Name |
|----------|------|
| Topic | `product-events` |
| Typical subscription | `product-events-sub` (for consumers) |

**Create topic & subscription (gcloud):**

```bash
export PROJECT_ID=your-project-id
gcloud config set project $PROJECT_ID
gcloud pubsub topics create product-events
gcloud pubsub subscriptions create product-events-sub \
  --topic=product-events \
  --ack-deadline=60
```

Grant the runtime service account **`roles/pubsub.publisher`** (project or topic level).

**Optional local emulator:** set `spring.cloud.gcp.pubsub.emulator-host` and run the Pub/Sub emulator; see [Google’s Pub/Sub emulator docs](https://cloud.google.com/pubsub/docs/emulator).

**Verify:** create a product via `POST /api/products`, then pull from the subscription or check logs.

## Outbox message envelope

Messages are JSON strings. Example:

```json
{
  "eventType": "PRODUCT_CREATED",
  "aggregateType": "PRODUCT",
  "aggregateId": "P1A2B3C4D5E6",
  "payload": {
    "productId": "P1A2B3C4D5E6",
    "eventType": "PRODUCT_CREATED",
    "version": 1,
    "updatedAt": "2025-02-14T10:00:00Z",
    "name": "Example",
    "description": null,
    "price": 99.99,
    "sku": "SKU-001",
    "stockQuantity": 50,
    "category": "Electronics"
  },
  "occurredAt": "...",
  "version": 1
}
```

`PRODUCT_DELETED` payloads omit product fields where applicable. Downstream services should treat events as **at-least-once** and use `productId` + **`version`** / **`updatedAt`** for idempotency.

## Versioning

| Store | Field | Behavior |
|-------|--------|----------|
| Firestore product | `version` | Incremented on every update |
| Outbox / Pub/Sub payload | `version`, `updatedAt` | Carried for indexer / search conflict rules |

## Operational notes

- **Multiple replicas:** the outbox job runs on every instance; consider a single leader, distributed lock, or external scheduler if duplicate publishes are unacceptable (Pub/Sub deduplication is not assumed here).
- **Large catalogs:** `GET /api/products` is unpaginated; add paging before very large datasets.
- **Credentials:** avoid committing real paths or JSON keys; use env-specific config or Workload Identity on GKE/GCE.

## Other services

This repository is **Firestore-only** for product data. Other platform services (auth, user, etc.) may use **PostgreSQL** or separate databases; their setup is not part of this service.

## License / project

Group: `com.mcart` — see `build.gradle` for version and dependencies.
