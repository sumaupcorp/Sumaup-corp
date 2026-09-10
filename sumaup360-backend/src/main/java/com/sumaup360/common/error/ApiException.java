package com.sumaup360.common.error;

import org.springframework.http.HttpStatus;

/**
 * Excepcion base de la aplicacion: lleva el HttpStatus con el que debe responderse.
 * Las subclases fijan el status; el GlobalExceptionHandler las traduce a ApiError.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
