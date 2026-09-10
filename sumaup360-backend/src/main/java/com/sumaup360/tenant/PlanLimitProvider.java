package com.sumaup360.tenant;

import java.util.UUID;

/**
 * Provee los limites del plan activo de un tenant (sucursales, usuarios). La implementacion
 * vive en billing; asi el modulo tenant no depende directamente de billing. Campos null =
 * sin limite (o sin licencia activa).
 */
public interface PlanLimitProvider {

    PlanLimits limits(UUID tenantId);

    record PlanLimits(Integer maxBranches, Integer maxUsers) {
        public static PlanLimits unlimited() {
            return new PlanLimits(null, null);
        }
    }
}
