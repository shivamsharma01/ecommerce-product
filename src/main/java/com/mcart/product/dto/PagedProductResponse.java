package com.mcart.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedProductResponse {
    private List<ProductResponse> items;
    private long total;
    private int page;
    private int size;
    private int totalPages;
}
