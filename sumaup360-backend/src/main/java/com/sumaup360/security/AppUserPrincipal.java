package com.sumaup360.security;

import com.sumaup360.auth.domain.UserType;

import java.util.Set;
import java.util.UUID;

/**
 * Principal autenticado que viaja en el SecurityContext.
 * Es la vista inmutable de "quien eres y que puedes hacer" segun el backend.
 */
public record AppUserPrincipal(
        UUID userId,
        String firebaseUid,
        String email,
        UserType userType,
        UUID tenantId,
        Set<String> permissions,
        Set<String> roles
) {
}
