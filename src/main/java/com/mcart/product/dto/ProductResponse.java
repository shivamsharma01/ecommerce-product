package com.mcart.product.dto;

import lombok.*;

import java.time.Instant;

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
    private String category;
    private Instant createdAt;
    private Instant updatedAt;
}
