package com.expensetracker.common.error;

/** One failed validation rule, returned in the {@code errors} array of a 400 response. */
public record FieldViolation(String field, String message) {}
