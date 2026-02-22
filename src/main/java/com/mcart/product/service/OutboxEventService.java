package com.mcart.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcart.product.dto.ProductEventPayload;
import com.mcart.product.model.OutboxEventDocument;
import com.mcart.product.model.ProductDocument;
import com.mcart.product.repository.OutboxFirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Persists outbox events for Pub/Sub publishing.
 * Events are picked up by {@link OutboxPublisherJob} and published to product-events topic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    public static final String AGGREGATE_PRODUCT = "PRODUCT";
    public static final String EVENT_PRODUCT_CREATED = "PRODUCT_CREATED";
    public static final String EVENT_PRODUCT_UPDATED = "PRODUCT_UPDATED";
    public static final String EVENT_PRODUCT_DELETED = "PRODUCT_DELETED";

    private final OutboxFirestoreRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publishProductCreated(ProductDocument product) throws JsonProcessingException {
        String payloadJson = objectMapper.writeValueAsString(toPayload(product, EVENT_PRODUCT_CREATED));
        saveOutboxEvent(product.getProductId(), EVENT_PRODUCT_CREATED, payloadJson);
    }

    public void publishProductUpdated(ProductDocument product) throws JsonProcessingException {
        String payloadJson = objectMapper.writeValueAsString(toPayload(product, EVENT_PRODUCT_UPDATED));
        saveOutboxEvent(product.getProductId(), EVENT_PRODUCT_UPDATED, payloadJson);
    }

    public void publishProductDeleted(String productId, long version, Instant updatedAt) throws JsonProcessingException {
        ProductEventPayload payload = ProductEventPayload.builder()
                .productId(productId)
                .eventType(EVENT_PRODUCT_DELETED)
                .version(version)
                .updatedAt(updatedAt)
                .build();
        saveOutboxEvent(productId, EVENT_PRODUCT_DELETED, objectMapper.writeValueAsString(payload));
    }

    private ProductEventPayload toPayload(ProductDocument product, String eventType) {
        return ProductEventPayload.builder()
                .productId(product.getProductId())
                .eventType(eventType)
                .version(product.getVersion())
                .updatedAt(product.getUpdatedAt())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .sku(product.getSku())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .build();
    }

    private void saveOutboxEvent(String aggregateId, String eventType, String payloadJson) {
        Instant now = Instant.now();
        OutboxEventDocument event = OutboxEventDocument.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType(AGGREGATE_PRODUCT)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payloadJson)
                .status("PENDING")
                .retryCount(0)
                .createdAt(now)
                .lastAttemptAt(now)
                .build();
        outboxRepository.save(event).block();
    }
}
