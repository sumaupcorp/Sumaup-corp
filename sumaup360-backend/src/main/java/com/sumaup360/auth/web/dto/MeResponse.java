package com.sumaup360.auth.web.dto;

import com.sumaup360.auth.domain.UserType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Vista de "quien soy y que puedo hacer" segun el backend.
 * branchId = sede asignada (null = todas); allowedModules = modulos visibles
 * (null = todos los habilitados). Ambos salen de la membresia del tenant actual.
 */
public record MeResponse(
        UUID userId,
        String firebaseUid,
        String email,
        UserType userType,
        UUID tenantId,
        Set<String> roles,
        Set<String> permissions,
        UUID branchId,
        List<String> allowedModules
) {
}
