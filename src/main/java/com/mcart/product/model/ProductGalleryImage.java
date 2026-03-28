package com.mcart.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductGalleryImage {
    private String thumbnailUrl;
    private String hdUrl;
    private String alt;
}

