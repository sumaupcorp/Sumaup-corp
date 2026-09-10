package com.sumaup360.security;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Acceso al principal autenticado desde la capa de servicio/web. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AppUserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new UnauthorizedException("No hay un usuario autenticado.");
        }
        return principal;
    }

    /**
     * Tenant del contexto del usuario. Falla si no tiene tenant (p. ej. staff sin membresia):
     * las operaciones de Negocios requieren un tenant activo.
     */
    public static UUID requireCurrentTenant() {
        UUID tenantId = currentPrincipal().tenantId();
        if (tenantId == null) {
            throw new BadRequestException(
                    "No hay un tenant activo en tu contexto. Asocia el usuario a un tenant primero.");
        }
        return tenantId;
    }
}
