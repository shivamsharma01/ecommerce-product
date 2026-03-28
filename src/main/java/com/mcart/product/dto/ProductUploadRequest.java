package com.mcart.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUploadRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    private Double price;

    @NotBlank
    @Size(max = 100)
    private String sku;

    @NotNull
    @Min(0)
    private Integer stockQuantity;

    @NotNull
    @Size(max = 20)
    private List<@NotBlank @Size(max = 100) String> categories;

    @Size(max = 100)
    private String brand;

    @NotNull
    @Size(min = 1, max = 10)
    private List<@Valid ProductUploadGalleryImageRequest> gallery;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    private Double rating;

    private Boolean inStock;
    private Map<String, Object> attributes;
}

