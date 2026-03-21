package com.mcart.product.repository;

import com.mcart.product.firestore.FirestoreStructuredQueries;
import com.mcart.product.model.ProductDocument;
import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import com.google.firestore.v1.StructuredQuery;
import com.google.protobuf.Int32Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ProductFirestoreRepository {

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

    /**
     * Indexed query on {@code sku} (single-field equality).
     */
    public Mono<ProductDocument> findFirstBySku(String sku) {
        StructuredQuery.Builder query = StructuredQuery.newBuilder()
                .setWhere(FirestoreStructuredQueries.stringFieldEquals("sku", sku))
                .setLimit(Int32Value.of(1));
        return firestoreTemplate.execute(query, ProductDocument.class).next();
    }

    public Mono<Boolean> existsBySku(String sku) {
        return findFirstBySku(sku).hasElement();
    }

    /**
     * {@code true} if another product (not {@code excludeProductId}) already uses this SKU.
     */
    public Mono<Boolean> skuTakenByOtherProduct(String sku, String excludeProductId) {
        return findFirstBySku(sku)
                .map(p -> !excludeProductId.equals(p.getProductId()))
                .defaultIfEmpty(false);
    }
}
