package com.mcart.product.firestore;

import com.google.firestore.v1.StructuredQuery;
import com.google.firestore.v1.Value;

/**
 * Small helpers for {@link com.google.cloud.spring.data.firestore.FirestoreTemplate#execute} queries.
 */
public final class FirestoreStructuredQueries {

    private FirestoreStructuredQueries() {
    }

    public static StructuredQuery.Filter stringFieldEquals(String fieldPath, String value) {
        return StructuredQuery.Filter.newBuilder()
                .setFieldFilter(StructuredQuery.FieldFilter.newBuilder()
                        .setField(StructuredQuery.FieldReference.newBuilder().setFieldPath(fieldPath).build())
                        .setOp(StructuredQuery.FieldFilter.Operator.EQUAL)
                        .setValue(Value.newBuilder().setStringValue(value).build())
                        .build())
                .build();
    }
}
