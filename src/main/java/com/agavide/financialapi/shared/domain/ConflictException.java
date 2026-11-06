package com.agavide.financialapi.shared.domain;

/**
 * Thrown when an operation is not allowed by the current state of an aggregate
 * (for example, cancelling an order that is no longer NEW). Mapped to HTTP 409.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
