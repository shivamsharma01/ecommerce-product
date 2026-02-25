# Product Service – Firestore, Outbox, Pub/Sub Implementation

## Architecture

```
┌────────────────────┐
│ Product Service    │
│  - Write Firestore │
│  - Outbox pattern  │
│  - Publish Event   │
└──────────┬─────────┘
           ↓
     Pub/Sub Topic (product-events)
           ↓
      Indexer (GKE)
           ↓
     Elasticsearch
```

## Product Service Changes

### Summary
- **Database:** Switched from PostgreSQL to Firestore
- **Outbox:** Firestore `outbox_events` collection (same transaction as product write)
- **Pub/Sub:** Events published to `product-events` topic
- **Version field:** Every product has `version`; incremented on each update

### New/Modified Components
| Component | Description |
|-----------|-------------|
| `ProductDocument` | Firestore entity with `productId`, `version`, `updatedAt` |
| `OutboxEventDocument` | Firestore outbox entity |
| `ProductFirestoreRepository` | Firestore repository for products |
| `OutboxFirestoreRepository` | Firestore repository for outbox |
| `OutboxEventService` | Persists outbox events on create/update/delete |
| `OutboxPublisherJob` | Scheduled job (5s) publishes pending outbox events to Pub/Sub |
| `ProductEventPayload` | Event payload DTO (productId, version, updatedAt, product fields) |

### API Changes
- Product ID is now a **string** (e.g. `P1`, `PABC123...`) instead of `Long`
- Create returns generated `productId` (e.g. `P` + 12-char hex)

### Configuration
- Removed PostgreSQL/JPA config
- Added Firestore and Pub/Sub (GCP project-id, emulator options)

---

## Product Indexer Changes

### Summary
- Supports **ProductEventEnvelope** (from product service outbox) and **CloudEventEnvelope** (Eventarc)
- **Idempotent:** Uses `productId` as Elasticsearch `_id`
- **Version handling:** Skips write if existing ES doc has higher `version` or newer `updatedAt`

### New/Modified Components
| Component | Description |
|-----------|-------------|
| `ProductEventEnvelope` | Envelope for outbox events |
| `ProductEventPayload` | Payload DTO (matches product service) |
| `ProductEventMapper` | Maps `ProductEventPayload` → ES `Product` |
| `ProductIndexerService.processMessage(String)` | Dispatches by format; version check before index |
| `Product` model | Added `version`, `updatedAt` fields |
| `ReindexService` | Full reindex: deletes index, recreates, indexes all from Firestore |
| `ReindexController` | POST /admin/reindex triggers reindex |

### Event Format (Outbox)
```json
{
  "eventType": "PRODUCT_CREATED",
  "aggregateType": "PRODUCT",
  "aggregateId": "P1",
  "payload": {
    "productId": "P1",
    "eventType": "PRODUCT_CREATED",
    "version": 5,
    "updatedAt": "2025-02-14T10:00:00Z",
    "name": "...",
    "description": "...",
    "price": 99.99,
    "sku": "SKU-001",
    "stockQuantity": 50,
    "category": "Electronics"
  },
  "occurredAt": "...",
  "version": 1
}
```

### Pub/Sub Setup
- **Topic:** `product-events`
- **Subscription:** `product-events-sub` (used by indexer)

### Reindex
```bash
curl -X POST http://localhost:8085/admin/reindex
```
Deletes the products index first, then indexes all from Firestore (no orphaned documents).

---

## Search Service Changes

### Summary
- Added `version` and `updatedAt` to `Product` model for index schema alignment
- No logic changes; search continues to work as before

---

## GCP Setup

1. **Firestore:** Create database; collections `products` and `outbox_events` are created on first write.
2. **Pub/Sub:** Create topic `product-events` and subscription `product-events-sub`.
3. **IAM:** Product service needs `roles/datastore.user`, `roles/pubsub.publisher`; indexer needs `roles/pubsub.subscriber`, `roles/pubsub.viewer`.

---

## Version Strategy

| Store | Field | Behavior |
|------|-------|----------|
| Firestore | `version` | Incremented on every update |
| Elasticsearch | `version`, `updatedAt` | Stored for conflict resolution |
| Reindex | — | If ES doc has newer `version` or `updatedAt` → skip overwrite |

This keeps reindex safe while the system is live.
