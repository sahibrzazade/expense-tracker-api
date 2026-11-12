package com.expensetracker.common.error;

/** Thrown when a request conflicts with the current state of a resource. Mapped to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
