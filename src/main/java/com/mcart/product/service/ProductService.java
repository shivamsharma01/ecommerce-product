package com.mcart.product.service;

import com.mcart.product.dto.ProductRequest;
import com.mcart.product.dto.ProductResponse;
import com.mcart.product.exception.DuplicateSkuException;
import com.mcart.product.exception.ProductNotFoundException;
import com.mcart.product.mapper.ProductMapper;
import com.mcart.product.model.ProductDocument;
import com.mcart.product.repository.ProductFirestoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductFirestoreRepository productRepository;
    private final OutboxEventService outboxEventService;
    private final ProductMapper productMapper;

    @Transactional(transactionManager = "firestoreTransactionManager")
    public Mono<ProductResponse> createProduct(ProductRequest request) {
        return productRepository.existsBySku(request.getSku())
                .flatMap(exists -> exists
                        ? Mono.error(new DuplicateSkuException(request.getSku()))
                        : doCreateProduct(request));
    }

    private Mono<ProductResponse> doCreateProduct(ProductRequest request) {
        String productId = "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        Date now = new Date();

        ProductDocument product = ProductDocument.builder()
                .productId(productId)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .sku(request.getSku())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .version(1L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return productRepository.save(product)
                .flatMap(p -> outboxEventService.publishProductCreated(p).thenReturn(p))
                .map(productMapper::toResponse);
    }

    public Mono<ProductResponse> getProductById(String id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .map(productMapper::toResponse);
    }

    public Flux<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .map(productMapper::toResponse);
    }

    @Transactional(transactionManager = "firestoreTransactionManager")
    public Mono<ProductResponse> updateProduct(String id, ProductRequest request) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .flatMap(product -> productRepository.skuTakenByOtherProduct(request.getSku(), id)
                        .flatMap(taken -> taken
                                ? Mono.error(new DuplicateSkuException(request.getSku()))
                                : Mono.just(product)))
                .flatMap(product -> {
                    product.setName(request.getName());
                    product.setDescription(request.getDescription());
                    product.setPrice(request.getPrice());
                    product.setSku(request.getSku());
                    product.setStockQuantity(request.getStockQuantity());
                    product.setCategory(request.getCategory());
                    product.setVersion(product.getVersion() + 1);
                    product.setUpdatedAt(new Date());
                    return productRepository.save(product);
                })
                .flatMap(p -> outboxEventService.publishProductUpdated(p).thenReturn(p))
                .map(productMapper::toResponse);
    }

    @Transactional(transactionManager = "firestoreTransactionManager")
    public Mono<Void> deleteProduct(String id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .flatMap(product -> outboxEventService.publishProductDeleted(id, product.getVersion(),
                                product.getUpdatedAt() != null ? product.getUpdatedAt().toInstant() : null)
                        .then(productRepository.deleteById(id)));
    }
}
