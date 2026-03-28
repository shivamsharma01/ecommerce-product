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
public class ProductUploadGalleryImageRequest {
    @NotBlank
    @Size(max = 255)
    private String thumbFile;

    @Size(max = 255)
    private String hdFile;

    @Size(max = 200)
    private String alt;
}

