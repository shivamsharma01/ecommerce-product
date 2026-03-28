package com.mcart.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GalleryImageRequest {

    @NotBlank
    @Size(max = 2000)
    private String thumbnailUrl;

    @Size(max = 2000)
    private String hdUrl;

    @Size(max = 200)
    private String alt;
}

