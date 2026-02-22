package com.mcart.product.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.mcart.product.model.OutboxEventDocument;
import com.mcart.product.repository.OutboxFirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Polls Firestore outbox and publishes pending events to Pub/Sub.
 * Mirrors auth service OutboxPublisherJob pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherJob {

    private static final String PRODUCT_EVENTS_TOPIC = "product-events";
    private static final int BATCH_SIZE = 20;

    private final OutboxFirestoreRepository outboxRepository;
    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void publishOutboxEvents() {
        outboxRepository.findPendingEvents(BATCH_SIZE)
                .doOnNext(this::publishEvent)
                .collectList()
                .block();
    }

    private void publishEvent(OutboxEventDocument event) {
        try {
            String message = buildMessage(event);
            pubSubTemplate.publish(PRODUCT_EVENTS_TOPIC, message);
            event.setStatus("SENT");
            event.setLastAttemptAt(java.time.Instant.now());
            outboxRepository.save(event).block();
        } catch (Exception ex) {
            event.setRetryCount(event.getRetryCount() != null ? event.getRetryCount() + 1 : 1);
            event.setLastAttemptAt(java.time.Instant.now());
            outboxRepository.save(event).block();
            log.warn("Failed to publish outbox event {}", event.getEventId(), ex);
        }
    }

    private String buildMessage(OutboxEventDocument event) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(event.getPayload(), Map.class);
        return objectMapper.writeValueAsString(Map.of(
                "eventType", event.getEventType(),
                "aggregateType", event.getAggregateType(),
                "aggregateId", event.getAggregateId(),
                "payload", payload,
                "occurredAt", event.getCreatedAt().toString(),
                "version", 1
        ));
    }
}
