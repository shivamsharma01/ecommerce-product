package com.mcart.product.task;

import com.fasterxml.jackson.core.type.TypeReference;
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
import reactor.core.publisher.Mono;

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

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

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

    private Mono<OutboxEventDocument> publishEvent(OutboxEventDocument event) {
        final String message;
        try {
            message = buildMessage(event);
        } catch (Exception ex) {
            log.warn("Failed to build Pub/Sub message for outbox event {}", event.getEventId(), ex);
            return bumpRetryAndSave(event);
        }

        return Mono.fromFuture(pubSubTemplate.publish(PRODUCT_EVENTS_TOPIC, message))
                .flatMap(ignored -> {
                    event.setStatus("SENT");
                    event.setLastAttemptAt(new Date());
                    return outboxRepository.save(event);
                })
                .onErrorResume(ex -> {
                    log.warn("Failed to publish outbox event {}", event.getEventId(), ex);
                    return bumpRetryAndSave(event);
                });
    }

    private Mono<OutboxEventDocument> bumpRetryAndSave(OutboxEventDocument event) {
        event.setRetryCount(event.getRetryCount() != null ? event.getRetryCount() + 1 : 1);
        event.setLastAttemptAt(new Date());
        return outboxRepository.save(event)
                .doOnError(saveEx -> log.error("Failed to update outbox event {} after publish failure",
                        event.getEventId(), saveEx))
                .onErrorResume(e -> Mono.empty());
    }

    private String buildMessage(OutboxEventDocument event) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(event.getPayload(), MAP_TYPE);
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
