package com.sumaup360.erp.module;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Provee el techo de modulos permitido por la licencia/plan activo de un tenant.
 * La implementacion vive en billing (asi erp.module no depende directamente de billing).
 * Optional.empty() = sin licencia activa => sin techo (no se recorta).
 */
public interface LicenseModuleProvider {
    Optional<Set<String>> allowedModules(UUID tenantId);
}
