package com.sumaup360.erp.lodging.enums;

/** Ciclo de vida de una estadia: reserva → check-in → check-out (o cancelada/no show). */
public enum StayStatus {
    RESERVED, CHECKED_IN, CHECKED_OUT, CANCELED, NO_SHOW
}
