package com.pact.common;

import org.springframework.http.HttpStatus;

/**
 * Thrown by service-layer code when a request can't be fulfilled.
 * Carries an HTTP status and a stable machine-readable code
 * (e.g. "EMAIL_ALREADY_REGISTERED") that GlobalExceptionHandler
 * maps onto the ApiError shape.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}