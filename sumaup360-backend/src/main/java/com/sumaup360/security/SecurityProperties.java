package com.sumaup360.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de seguridad propias del backend (prefijo `security`).
 *
 * devMode: SOLO para desarrollo local. Si esta activo y Firebase no esta configurado,
 * se acepta el header X-Debug-Uid para simular un usuario. NUNCA activar en produccion.
 *
 * bootstrapAdminUid: si coincide con el uid que se provisiona (en devMode), se le asigna
 * el rol staff `admin` para poder operar el sistema desde cero.
 */
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private boolean devMode = false;
    private String bootstrapAdminUid = "";

    public boolean isDevMode() {
        return devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public String getBootstrapAdminUid() {
        return bootstrapAdminUid;
    }

    public void setBootstrapAdminUid(String bootstrapAdminUid) {
        this.bootstrapAdminUid = bootstrapAdminUid;
    }
}
