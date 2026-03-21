package com.mcart.product.exception;

/**
 * Raised when an outbox row could not be written or serialized.
 * Clients may retry; see HTTP mapping in {@link GlobalExceptionHandler}.
 */
public class OutboxPersistenceException extends RuntimeException {

    public OutboxPersistenceException(String message) {
        super(message);
    }

    public OutboxPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
