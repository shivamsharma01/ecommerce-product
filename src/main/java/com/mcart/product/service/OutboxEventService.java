package com.mcart.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcart.product.dto.ProductEventPayload;
import com.mcart.product.dto.GalleryImageResponse;
import com.mcart.product.exception.OutboxPersistenceException;
import com.mcart.product.model.OutboxEventDocument;
import com.mcart.product.model.ProductDocument;
import com.mcart.product.repository.OutboxFirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
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

    public Mono<Void> publishProductCreated(ProductDocument product) {
        try {
            String payloadJson = objectMapper.writeValueAsString(toPayload(product, EVENT_PRODUCT_CREATED));
            return saveOutboxEvent(product.getProductId(), EVENT_PRODUCT_CREATED, payloadJson);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize outbox payload for product {}", product.getProductId(), ex);
            return Mono.error(new OutboxPersistenceException("Failed to serialize product event", ex));
        }
    }

    public Mono<Void> publishProductUpdated(ProductDocument product) {
        try {
            String payloadJson = objectMapper.writeValueAsString(toPayload(product, EVENT_PRODUCT_UPDATED));
            return saveOutboxEvent(product.getProductId(), EVENT_PRODUCT_UPDATED, payloadJson);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize outbox payload for product {}", product.getProductId(), ex);
            return Mono.error(new OutboxPersistenceException("Failed to serialize product event", ex));
        }
    }

    public Mono<Void> publishProductDeleted(String productId, long version, Instant updatedAt) {
        try {
            ProductEventPayload payload = ProductEventPayload.builder()
                    .productId(productId)
                    .eventType(EVENT_PRODUCT_DELETED)
                    .version(version)
                    .updatedAt(updatedAt)
                    .build();
            return saveOutboxEvent(productId, EVENT_PRODUCT_DELETED, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize outbox payload for product delete {}", productId, ex);
            return Mono.error(new OutboxPersistenceException("Failed to serialize product delete event", ex));
        }
    }

    private ProductEventPayload toPayload(ProductDocument product, String eventType) {
        return ProductEventPayload.builder()
                .productId(product.getProductId())
                .eventType(eventType)
                .version(product.getVersion())
                .updatedAt(product.getUpdatedAt() != null ? product.getUpdatedAt().toInstant() : null)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .sku(product.getSku())
                .stockQuantity(product.getStockQuantity())
                .categories(product.getCategories())
                .brand(product.getBrand())
                .gallery(toGalleryResponse(product))
                .rating(product.getRating())
                .inStock(product.getInStock())
                .attributes(product.getAttributes())
                .build();
    }

    private java.util.List<GalleryImageResponse> toGalleryResponse(ProductDocument product) {
        java.util.List<GalleryImageResponse> out = new ArrayList<>();
        if (product.getGallery() == null) {
            return out;
        }
        for (var item : product.getGallery()) {
            out.add(GalleryImageResponse.builder()
                    .thumbnailUrl(item.getThumbnailUrl())
                    .hdUrl(item.getHdUrl())
                    .alt(item.getAlt())
                    .build());
        }
        return out;
    }

    private Mono<Void> saveOutboxEvent(String aggregateId, String eventType, String payloadJson) {
        java.util.Date now = new java.util.Date();
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
        return outboxRepository.save(event)
                .then()
                .onErrorMap(ex -> ex instanceof OutboxPersistenceException ope
                        ? ope
                        : new OutboxPersistenceException("Failed to persist outbox event", ex));
    }
}
