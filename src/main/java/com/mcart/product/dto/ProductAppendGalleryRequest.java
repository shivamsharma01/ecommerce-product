package com.mcart.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAppendGalleryRequest {

    @NotEmpty
    @Valid
    private List<ProductUploadGalleryImageRequest> gallery;
}
