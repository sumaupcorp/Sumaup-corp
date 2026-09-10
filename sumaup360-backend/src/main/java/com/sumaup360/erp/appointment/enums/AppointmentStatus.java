package com.sumaup360.erp.appointment.enums;

/** Estado de una cita. REQUESTED = reserva online pendiente de confirmar por el negocio. */
public enum AppointmentStatus {
    REQUESTED,
    SCHEDULED,
    CONFIRMED,
    COMPLETED,
    CANCELED,
    NO_SHOW
}
