package com.mcart.product.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.mcart.product.model.OutboxEventDocument;
import com.mcart.product.repository.OutboxFirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.Map;

/**
 * Polls Firestore outbox and publishes pending events to Pub/Sub.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.cloud.gcp.firestore.enabled", havingValue = "true")
public class OutboxPublisherJob {

    private static final String PRODUCT_EVENTS_TOPIC = "product-events";
    private static final int BATCH_SIZE = 20;

    private final OutboxFirestoreRepository outboxRepository;
    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void publishOutboxEvents() {
        outboxRepository.findPendingEvents(BATCH_SIZE)
                .flatMap(this::publishEvent)
                .onErrorResume(ex -> {
                    log.error("Outbox publisher failed, will retry next run", ex);
                    return Flux.empty();
                })
                .subscribe();
    }

    private Flux<OutboxEventDocument> publishEvent(OutboxEventDocument event) {
        try {
            String message = buildMessage(event);
            pubSubTemplate.publish(PRODUCT_EVENTS_TOPIC, message);
            event.setStatus("SENT");
            event.setLastAttemptAt(new Date());
            return outboxRepository.save(event).flux();
        } catch (Exception ex) {
            log.warn("Failed to publish outbox event {}", event.getEventId(), ex);
            event.setRetryCount(event.getRetryCount() != null ? event.getRetryCount() + 1 : 1);
            event.setLastAttemptAt(new Date());
            return outboxRepository.save(event)
                    .doOnError(saveEx -> log.error("Failed to update outbox event {} after publish failure", event.getEventId(), saveEx))
                    .onErrorResume(e -> reactor.core.publisher.Mono.empty())
                    .flux();
        }
    }

    private String buildMessage(OutboxEventDocument event) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(event.getPayload(), Map.class);
        return objectMapper.writeValueAsString(Map.of(
                "eventType", event.getEventType(),
                "aggregateType", event.getAggregateType(),
                "aggregateId", event.getAggregateId(),
                "payload", payload,
                "occurredAt", event.getCreatedAt() != null ? event.getCreatedAt().toString() : null,
                "version", 1
        ));
    }
}
