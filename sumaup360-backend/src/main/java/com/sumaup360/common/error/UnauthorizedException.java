package com.sumaup360.common.error;

import org.springframework.http.HttpStatus;

/** No autenticado o token invalido (401). */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
