package com.sumaup360.app.taxi.enums;

/** Estados de una solicitud de comprobante (QR taxista). */
public enum RequestStatus {
    PENDIENTE_TAXISTA,
    CONFIRMADO_TAXISTA,
    MONTO_EDITADO_CONFIRMADO,
    RECHAZADO_TAXISTA,
    PENDIENTE_BACKOFFICE,
    EN_PROCESO_BACKOFFICE,
    COMPLETADO,
    OBSERVADO,
    CANCELADO
}
