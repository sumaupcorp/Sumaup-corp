package com.sumaup360.erp.einvoicing.nubefact;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuracion global de NubeFact. Las credenciales REALES viven por empresa
 * (erp.company_einvoicing_config, token cifrado). Estos valores son un fallback
 * DEMO opcional para pruebas: si una empresa no tiene credenciales propias y hay
 * demo-ruta/demo-token definidos, se emite contra el entorno de demostracion.
 */
@Component
@ConfigurationProperties(prefix = "nubefact")
public class NubefactProperties {

    private boolean enabled = true;
    private String demoRuta = "";
    private String demoToken = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDemoRuta() {
        return demoRuta;
    }

    public void setDemoRuta(String demoRuta) {
        this.demoRuta = demoRuta;
    }

    public String getDemoToken() {
        return demoToken;
    }

    public void setDemoToken(String demoToken) {
        this.demoToken = demoToken;
    }
}
