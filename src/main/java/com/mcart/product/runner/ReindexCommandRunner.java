package com.mcart.product.runner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.mcart.product.dto.ProductEventPayload;
import com.mcart.product.model.ProductDocument;
import com.mcart.product.repository.ProductFirestoreRepository;
import com.mcart.product.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Batch reindex job: run with --reindex to read all products from Firestore
 * and publish events to Pub/Sub for indexing. Indexer consumes and writes to ES
 * with version check (skips overwriting newer docs).
 *
 * Usage: java -jar app.jar --reindex
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.reindex", havingValue = "true")
public class ReindexCommandRunner implements CommandLineRunner {

    private static final String PRODUCT_EVENTS_TOPIC = "product-events";

    private final ProductFirestoreRepository productRepository;
    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        log.info("Starting product reindex job");
        Long count = productRepository.findAll()
                .doOnNext(this::publishProductEvent)
                .count()
                .block();
        log.info("Reindex complete: published {} product events", count);
    }

    private void publishProductEvent(ProductDocument product) {
        try {
            ProductEventPayload payload = ProductEventPayload.builder()
                    .productId(product.getProductId())
                    .eventType(OutboxEventService.EVENT_PRODUCT_UPDATED)
                    .version(product.getVersion())
                    .updatedAt(product.getUpdatedAt())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .sku(product.getSku())
                    .stockQuantity(product.getStockQuantity())
                    .category(product.getCategory())
                    .build();
            String message = objectMapper.writeValueAsString(Map.of(
                    "eventType", OutboxEventService.EVENT_PRODUCT_UPDATED,
                    "aggregateType", OutboxEventService.AGGREGATE_PRODUCT,
                    "aggregateId", product.getProductId(),
                    "payload", payload,
                    "occurredAt", java.time.Instant.now().toString(),
                    "version", 1
            ));
            pubSubTemplate.publish(PRODUCT_EVENTS_TOPIC, message);
            log.debug("Published reindex event for product {}", product.getProductId());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish reindex event for product {}", product.getProductId(), e);
        }
    }
}
