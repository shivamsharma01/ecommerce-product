package com.mcart.product.repository;

import com.mcart.product.firestore.FirestoreStructuredQueries;
import com.mcart.product.model.OutboxEventDocument;
import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import com.google.firestore.v1.StructuredQuery;
import com.google.protobuf.Int32Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class OutboxFirestoreRepository {

    private static final String STATUS_PENDING = "PENDING";

    private final FirestoreTemplate firestoreTemplate;

    public Mono<OutboxEventDocument> save(OutboxEventDocument event) {
        return firestoreTemplate.save(event);
    }

    /**
     * Indexed query: {@code status == PENDING} ordered by {@code createdAt}.
     * Requires a composite index on {@code (status, createdAt)} — see {@code firestore.indexes.json}.
     */
    public Flux<OutboxEventDocument> findPendingEvents(int limit) {
        StructuredQuery.Builder query = StructuredQuery.newBuilder()
                .setWhere(FirestoreStructuredQueries.stringFieldEquals("status", STATUS_PENDING))
                .addOrderBy(StructuredQuery.Order.newBuilder()
                        .setField(StructuredQuery.FieldReference.newBuilder().setFieldPath("createdAt").build())
                        .setDirection(StructuredQuery.Direction.ASCENDING)
                        .build())
                .setLimit(Int32Value.of(limit));
        return firestoreTemplate.execute(query, OutboxEventDocument.class);
    }
}
