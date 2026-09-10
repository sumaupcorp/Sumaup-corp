package com.sumaup360.common.error;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Cuerpo de error uniforme para toda la API.
 * fieldErrors solo se usa en errores de validacion.
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }
}
