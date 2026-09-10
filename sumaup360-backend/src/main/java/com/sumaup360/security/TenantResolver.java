package com.sumaup360.security;

import java.util.UUID;

/**
 * Resuelve el tenant por defecto de un usuario. La implementacion vive en el modulo tenant
 * (membresia); asi el modulo auth no depende directamente de tenant. Devuelve null si el
 * usuario no tiene tenant (p. ej. Personas o staff sin membresia).
 */
public interface TenantResolver {
    UUID resolveDefaultTenant(UUID userId);
}
