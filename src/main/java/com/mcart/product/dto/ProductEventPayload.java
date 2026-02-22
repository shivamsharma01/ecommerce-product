package com.mcart.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payload for product events published to Pub/Sub.
 * Consumed by product-indexer for Elasticsearch indexing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductEventPayload {

    private String productId;
    private String eventType;   // PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED
    private long version;
    private Instant updatedAt;

    // Product data (null for DELETE)
    private String name;
    private String description;
    private BigDecimal price;
    private String sku;
    private Integer stockQuantity;
    private String category;
}
