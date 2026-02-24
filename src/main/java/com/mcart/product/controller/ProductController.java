package com.mcart.product.controller;

import com.mcart.product.dto.ProductRequest;
import com.mcart.product.dto.ProductResponse;
import com.mcart.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public Mono<ResponseEntity<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ProductResponse>> getProduct(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Mono<ResponseEntity<List<ProductResponse>>> getAllProducts() {
        return productService.getAllProducts().collectList()
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable String id) {
        return productService.deleteProduct(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
}
