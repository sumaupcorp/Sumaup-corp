package com.sumaup360.erp.documenttemplate.enums;

/**
 * Estado del documento emitido.
 * DRAFT/ISSUED/CANCELLED/VOIDED son de uso interno; SENT/ACCEPTED/REJECTED quedan preparados
 * para el ciclo de facturacion electronica con SUNAT (TODO).
 */
public enum DocumentStatus {
    DRAFT,
    ISSUED,
    CANCELLED,
    VOIDED,
    SENT,
    ACCEPTED,
    REJECTED
}
