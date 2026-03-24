package com.mcart.product.controller;

import com.mcart.product.dto.ProductRequest;
import com.mcart.product.dto.ProductResponse;
import com.mcart.product.dto.PagedProductResponse;
import com.mcart.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static com.mcart.product.config.OpenApiConfig.BEARER_JWT;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create product")
    @SecurityRequirement(name = BEARER_JWT)
    public Mono<ResponseEntity<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by id")
    public Mono<ResponseEntity<ProductResponse>> getProduct(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    @Operation(summary = "List products (paginated)")
    public Mono<ResponseEntity<PagedProductResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getProductsPage(page, size)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    @SecurityRequirement(name = BEARER_JWT)
    public Mono<ResponseEntity<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product")
    @SecurityRequirement(name = BEARER_JWT)
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable String id) {
        return productService.deleteProduct(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
}
