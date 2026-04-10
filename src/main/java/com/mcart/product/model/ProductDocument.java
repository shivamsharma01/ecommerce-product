package com.mcart.product.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.IgnoreExtraProperties;
import com.google.cloud.spring.data.firestore.Document;
import lombok.*;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
@Document(collectionName = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    /** Firestore document id (path); must not duplicate a stored field named the same as this property. */
    @DocumentId
    private String id;

    private String name;
    private String description;
    private Double price;
    private String sku;
    private Integer stockQuantity;
    private List<String> categories;
    private String brand;
    private List<ProductGalleryImage> gallery;
    private Double rating;
    private Boolean inStock;
    private Map<String, Object> attributes;

    /** Version incremented on every update. Used for ES external version / conflict resolution. */
    private long version;

    private java.util.Date createdAt;
    private java.util.Date updatedAt;
}
