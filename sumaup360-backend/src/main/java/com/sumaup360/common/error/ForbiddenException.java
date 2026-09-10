package com.sumaup360.common.error;

import org.springframework.http.HttpStatus;

/** Autenticado pero sin permiso (403). */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
