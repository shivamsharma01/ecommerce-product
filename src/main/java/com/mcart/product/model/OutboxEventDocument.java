package com.mcart.product.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.*;

import java.time.Instant;

/**
 * Firestore outbox document for reliable event publishing.
 * Written in same transaction as product write; publisher job publishes to Pub/Sub.
 */
@Document(collectionName = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEventDocument {

    @DocumentId
    private String eventId;

    private String aggregateType;  // PRODUCT
    private String aggregateId;     // productId
    private String eventType;       // PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED
    private String payload;         // JSON
    private String status;         // PENDING, SENT
    private Integer retryCount;
    private Instant createdAt;
    private Instant lastAttemptAt;
}
