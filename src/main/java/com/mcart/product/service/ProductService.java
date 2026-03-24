package com.mcart.product.service;

import com.mcart.product.dto.ProductRequest;
import com.mcart.product.dto.ProductResponse;
import com.mcart.product.dto.PagedProductResponse;
import com.mcart.product.exception.DuplicateSkuException;
import com.mcart.product.exception.ProductNotFoundException;
import com.mcart.product.mapper.ProductMapper;
import com.mcart.product.model.ProductDocument;
import com.mcart.product.repository.ProductFirestoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Comparator;
import java.util.List;
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
                .categories(request.getCategories())
                .brand(request.getBrand())
                .imageUrls(request.getImageUrls())
                .rating(request.getRating())
                .inStock(resolveInStock(request))
                .attributes(request.getAttributes())
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

    public Mono<PagedProductResponse> getProductsPage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int skip = safePage * safeSize;

        return productRepository.findAll()
                .sort(Comparator.comparing(ProductDocument::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(productMapper::toResponse)
                .collectList()
                .map(items -> toPage(items, safePage, safeSize, skip));
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
                    product.setCategories(request.getCategories());
                    product.setBrand(request.getBrand());
                    product.setImageUrls(request.getImageUrls());
                    product.setRating(request.getRating());
                    product.setInStock(resolveInStock(request));
                    product.setAttributes(request.getAttributes());
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

    private Boolean resolveInStock(ProductRequest request) {
        if (request.getInStock() != null) {
            return request.getInStock();
        }
        return request.getStockQuantity() != null && request.getStockQuantity() > 0;
    }

    private PagedProductResponse toPage(List<ProductResponse> items, int page, int size, int skip) {
        int total = items.size();
        int from = Math.min(skip, total);
        int to = Math.min(from + size, total);
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return PagedProductResponse.builder()
                .items(items.subList(from, to))
                .total(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }
}
