package com.mcart.product.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Firestore document for products.
 * Uses version field for optimistic concurrency and safe reindexing.
 */
@Document(collectionName = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @DocumentId
    private String productId;

    private String name;
    private String description;
    private BigDecimal price;
    private String sku;
    private Integer stockQuantity;
    private String category;

    /** Version incremented on every update. Used for ES external version / conflict resolution. */
    private long version;

    private Instant createdAt;
    private Instant updatedAt;
}
