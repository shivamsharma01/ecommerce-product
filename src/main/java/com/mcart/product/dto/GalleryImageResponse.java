package com.mcart.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GalleryImageResponse {
    private String thumbnailUrl;
    private String hdUrl;
    private String alt;
}

