package com.mcart.product.service;

import com.mcart.product.dto.ProductRequest;
import com.mcart.product.dto.ProductResponse;
import com.mcart.product.mapper.ProductMapper;
import com.mcart.product.model.ProductDocument;
import com.mcart.product.repository.ProductFirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductFirestoreRepository productRepository;
    private final OutboxEventService outboxEventService;
    private final ProductMapper productMapper;

    public ProductResponse createProduct(ProductRequest request) {
        if (Boolean.TRUE.equals(productRepository.existsBySku(request.getSku()).block())) {
            throw new IllegalArgumentException("Product with SKU " + request.getSku() + " already exists");
        }

        String productId = "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        Instant now = Instant.now();

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

        product = productRepository.save(product).block();
        try {
            outboxEventService.publishProductCreated(product);
        } catch (Exception ex) {
            log.warn("Failed to persist outbox event for product {} - will be retried by publisher", productId, ex);
        }

        return productMapper.toResponse(product);
    }

    public ProductResponse getProductById(String id) {
        ProductDocument product = productRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Product not found with id: " + id)))
                .block();
        return productMapper.toResponse(product);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .map(productMapper::toResponse)
                .collectList()
                .block();
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        ProductDocument product = productRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Product not found with id: " + id)))
                .block();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSku(request.getSku());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setVersion(product.getVersion() + 1);
        product.setUpdatedAt(Instant.now());

        product = productRepository.save(product).block();
        try {
            outboxEventService.publishProductUpdated(product);
        } catch (Exception ex) {
            log.warn("Failed to persist outbox event for product {} - will be retried by publisher", id, ex);
        }

        return productMapper.toResponse(product);
    }

    public void deleteProduct(String id) {
        ProductDocument product = productRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Product not found with id: " + id)))
                .block();

        productRepository.deleteById(id).block();
        try {
            outboxEventService.publishProductDeleted(id, product.getVersion(), product.getUpdatedAt());
        } catch (Exception ex) {
            log.warn("Failed to persist outbox event for product delete {} - will be retried by publisher", id, ex);
        }
    }
}
