package com.sumaup360.common.error;

import org.springframework.http.HttpStatus;

/** Recurso inexistente (404). */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
