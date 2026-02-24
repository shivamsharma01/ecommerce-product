package com.mcart.product.repository;

import com.mcart.product.model.OutboxEventDocument;
import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;

@Repository
@RequiredArgsConstructor
public class OutboxFirestoreRepository {

    private final FirestoreTemplate firestoreTemplate;

    public Mono<OutboxEventDocument> save(OutboxEventDocument event) {
        return firestoreTemplate.save(event);
    }

    public Flux<OutboxEventDocument> findPendingEvents(int limit) {
        return firestoreTemplate.findAll(OutboxEventDocument.class)
                .filter(e -> "PENDING".equals(e.getStatus()))
                .sort(Comparator.comparing(OutboxEventDocument::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .take(limit);
    }
}
