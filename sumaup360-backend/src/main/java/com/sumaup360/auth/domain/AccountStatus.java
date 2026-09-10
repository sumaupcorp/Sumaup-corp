package com.sumaup360.auth.domain;

/** Estado de la cuenta efectiva en el backend. */
public enum AccountStatus {
    /** Recien provisionada desde Firebase; aun sin completar onboarding/roles. */
    PENDING,
    ACTIVE,
    SUSPENDED
}
