package com.sumaup360.security;

import java.util.UUID;

/**
 * Contexto de tenant por request (ThreadLocal). Lo establece el filtro de seguridad a partir
 * del usuario autenticado. La capa de datos lo usa para forzar el aislamiento por tenant_id.
 *
 * En Fase 1 aun no hay vinculo usuario-tenant persistido; el valor sera null hasta que se
 * implemente la membresia de tenant. La infraestructura ya queda lista.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
