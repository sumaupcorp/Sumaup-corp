package com.sumaup360.common.error;

import org.springframework.http.HttpStatus;

/** Conflicto de estado (409), p. ej. duplicado. */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
