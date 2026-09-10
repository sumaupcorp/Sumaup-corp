package com.sumaup360.auth.domain;

/**
 * Linea/identidad del usuario. Las tres NO se mezclan.
 * Un mismo Firebase puede, en teoria, tener mas de un contexto; el backend los separa.
 */
public enum UserType {
    /** Linea Personas (app movil). */
    PERSON,
    /** Linea Negocios (SaaS / ERP), trabajador de un tenant. */
    BUSINESS,
    /** Linea Interna (Backoffice SUMAUP), personal propio. */
    STAFF
}
