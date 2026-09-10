package com.sumaup360.erp.restaurant.enums;

/** Ciclo de vida de la comanda. */
public enum OrderStatus {
    OPEN,        // creada, recibiendo items
    IN_KITCHEN,  // enviada a cocina
    READY,       // lista para servir
    SERVED,      // servida en mesa
    BILLED,      // cobrada (genero venta)
    CANCELLED
}
