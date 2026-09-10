package com.sumaup360.erp.documenttemplate.dto;

/**
 * Metadatos del PDF generado. El endpoint de render devuelve los bytes del PDF directamente;
 * este DTO se usa para variantes que necesiten una respuesta JSON (p. ej. base64 a futuro).
 */
public record DocumentRenderResponse(
        String fileName,
        String contentType,
        int sizeBytes
) {
}
