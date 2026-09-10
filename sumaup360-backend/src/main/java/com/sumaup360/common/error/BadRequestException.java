package com.sumaup360.common.error;

import org.springframework.http.HttpStatus;

/** Solicitud invalida por reglas de negocio (400). */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
