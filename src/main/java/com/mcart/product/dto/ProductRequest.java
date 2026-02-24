package com.mcart.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2)
    private Double price;

    @NotBlank(message = "SKU is required")
    @Size(max = 100)
    private String sku;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotBlank(message = "Category is required")
    @Size(max = 100)
    private String category;
}
