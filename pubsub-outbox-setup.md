# Pub/Sub Setup for Outbox Pattern

This guide covers setting up Google Cloud Pub/Sub so the product service outbox publisher can publish events to the `product-events` topic.

---

## 1. GCP Prerequisites

- A GCP project (you have `my-project-chat-ai-487408`)
- A service account with credentials JSON (you have `~/.gcp/my-project-chat-ai-487408-5604b2cd4107.json`)
- [gcloud CLI](https://cloud.google.com/sdk/docs/install) installed (optional but recommended)

---

## 2. Create Pub/Sub Topic and Subscription

### Option A: Using gcloud CLI

```bash
# Set your project
export PROJECT_ID=my-project-chat-ai-487408
gcloud config set project $PROJECT_ID

# Create the topic (product service publishes here)
gcloud pubsub topics create product-events

# Create subscription (for indexer/consumers to pull messages)
gcloud pubsub subscriptions create product-events-sub \
  --topic=product-events \
  --ack-deadline=60
```

### Option B: Using GCP Console

1. Go to [Cloud Console → Pub/Sub → Topics](https://console.cloud.google.com/cloudpubsub/topic/list)
2. Click **Create Topic**
3. Topic ID: `product-events` → Create
4. Go to **Subscriptions** → **Create Subscription**
5. Subscription ID: `product-events-sub`
6. Topic: `product-events`
7. Ack deadline: 60 seconds (or as needed)
8. Create

---

## 3. IAM Permissions

The service account used by the product service needs permission to **publish** to the topic.

### Grant Publisher Role

```bash
# Get your service account email from the credentials JSON, or use:
export SA_EMAIL="your-sa-name@my-project-chat-ai-487408.iam.gserviceaccount.com"

# Grant Pub/Sub Publisher role (project-level is enough for topic publish)
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:$SA_EMAIL" \
  --role="roles/pubsub.publisher"
```

**Or** grant at topic level:

```bash
gcloud pubsub topics add-iam-policy-binding product-events \
  --member="serviceAccount:$SA_EMAIL" \
  --role="roles/pubsub.publisher"
```

### Verify Service Account

Check the `client_email` in your credentials file:

```bash
grep client_email /home/shivam-sharma/.gcp/my-project-chat-ai-487408-5604b2cd4107.json
```

Use that email as `SA_EMAIL` in the commands above.

---

## 4. Application Properties

Your existing `application.properties` already has GCP config that Pub/Sub uses:

```properties
spring.cloud.gcp.project-id=my-project-chat-ai-487408
spring.cloud.gcp.credentials.location=file:/home/shivam-sharma/.gcp/my-project-chat-ai-487408-5604b2cd4107.json
```

**Optional** Pub/Sub-specific overrides (only if needed):

```properties
# Enable Pub/Sub (default: true when starter is on classpath)
spring.cloud.gcp.pubsub.enabled=true

# Use same project/credentials as Firestore (inherited from spring.cloud.gcp.*)
# Override only if Pub/Sub uses a different project:
# spring.cloud.gcp.pubsub.project-id=my-project-chat-ai-487408
# spring.cloud.gcp.pubsub.credentials.location=file:/path/to/credentials.json
```

**For local development with emulator** (optional):

```properties
spring.cloud.gcp.pubsub.emulator-host=localhost:8085
```

Then run the emulator: `gcloud beta emulators pubsub start --project=my-project-chat-ai-487408`

---

## 5. How It Works

| Component | Role |
|-----------|------|
| **Product Service** | Publisher. `OutboxPublisherJob` polls Firestore `outbox_events` every 5s and publishes to `product-events` topic. |
| **Topic** | `product-events` – receives product CRUD events. |
| **Subscription** | `product-events-sub` – used by indexer (or other consumers) to pull messages. |

### Event Flow

1. Product create/update/delete → write to Firestore + outbox event.
2. `OutboxPublisherJob` (every 5s) → reads PENDING events → publishes to Pub/Sub → marks SENT.
3. Indexer subscribes to `product-events-sub` → processes events → updates Elasticsearch.

---

## 6. Verify Setup

### Publish a test message (gcloud)

```bash
gcloud pubsub topics publish product-events --message='{"test": true}'
```

### Pull a message (gcloud)

```bash
gcloud pubsub subscriptions pull product-events-sub --auto-ack
```

### From the product service

1. Start the app: `./gradlew bootRun`
2. Create a product via `POST /api/products`
3. Check logs for successful publish, or use the subscription pull above to see the event.

---

## 7. Troubleshooting

| Issue | Check |
|-------|-------|
| `PermissionDenied` / 403 | Service account needs `roles/pubsub.publisher` |
| Topic not found | Create topic: `gcloud pubsub topics create product-events` |
| Credentials not found | Verify `spring.cloud.gcp.credentials.location` path exists |
| Emulator not working | Ensure `spring.cloud.gcp.pubsub.emulator-host` is set and emulator is running |

---

## Summary Checklist

- [ ] Topic `product-events` created
- [ ] Subscription `product-events-sub` created (for indexer)
- [ ] Service account has `roles/pubsub.publisher`
- [ ] `application.properties` has `project-id` and `credentials.location`
- [ ] Product service running; create a product and confirm events in Pub/Sub
