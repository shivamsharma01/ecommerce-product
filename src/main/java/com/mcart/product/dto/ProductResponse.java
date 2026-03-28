package com.mcart.product.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private String id;
    private String name;
    private String description;
    private Double price;
    private String sku;
    private Integer stockQuantity;
    private List<String> categories;
    private String brand;
    private List<GalleryImageResponse> gallery;
    private Double rating;
    private Boolean inStock;
    private Map<String, Object> attributes;
    private Instant createdAt;
    private Instant updatedAt;
}
