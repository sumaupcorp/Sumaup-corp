package com.sumaup360.notification.domain;

/** Audiencia de una campana de push (usuarios de la app movil, Linea Personas). */
public enum CampaignAudience {
    /** Todos los usuarios con perfil de persona. */
    ALL,
    TAXISTA,
    DELIVERY_PEYA,
    SERVICIOS_PROFESIONALES,
    /** Usuarios que no completaron el onboarding (wizard). */
    ONBOARDING_INCOMPLETE,
    /** Usuarios con la orientacion tributaria pendiente. */
    ORIENTATION_PENDING
}
