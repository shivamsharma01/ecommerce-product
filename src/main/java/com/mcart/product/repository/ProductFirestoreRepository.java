package com.mcart.product.repository;

import com.mcart.product.model.ProductDocument;
import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ProductFirestoreRepository {

    private static final String PRODUCTS_COLLECTION = "products";

    private final FirestoreTemplate firestoreTemplate;

    public Mono<ProductDocument> save(ProductDocument product) {
        return firestoreTemplate.save(product);
    }

    public Mono<ProductDocument> findById(String productId) {
        return firestoreTemplate.findById(Mono.just(productId), ProductDocument.class);
    }

    public Flux<ProductDocument> findAll() {
        return firestoreTemplate.findAll(ProductDocument.class);
    }

    public Mono<Void> deleteById(String productId) {
        return firestoreTemplate.deleteById(Mono.just(productId), ProductDocument.class);
    }

    public Mono<Boolean> existsBySku(String sku) {
        return firestoreTemplate.findAll(ProductDocument.class)
                .filter(p -> sku.equals(p.getSku()))
                .hasElements();
    }
}
