package com.mcart.product.controller;

import com.mcart.product.dto.ProductAppendGalleryRequest;
import com.mcart.product.dto.ProductResponse;
import com.mcart.product.dto.ProductRequest;
import com.mcart.product.dto.ProductUploadRequest;
import com.mcart.product.dto.PagedProductResponse;
import com.mcart.product.service.ProductImageUploadService;
import com.mcart.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductImageUploadService productImageUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ProductResponse>> createProductWithUpload(
            @Valid @RequestPart("product") ProductUploadRequest request,
            @RequestPart("files") List<FilePart> files
    ) {
        return productImageUploadService.createProductWithImages(request, files)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ProductResponse>> getProduct(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Mono<ResponseEntity<PagedProductResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getProductsPage(page, size)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/{id}/gallery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ProductResponse>> appendProductGallery(
            @PathVariable String id,
            @Valid @RequestPart("gallery") ProductAppendGalleryRequest manifest,
            @RequestPart("files") List<FilePart> files
    ) {
        return productImageUploadService.appendGalleryImages(id, manifest.getGallery(), files)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable String id) {
        return productService.deleteProduct(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
}
