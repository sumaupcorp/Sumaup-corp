package com.sumaup360.app.sunat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuracion del microservicio externo de consulta RUC (sumaup360-sunat).
 * Prefijo `sunat`. Si enabled=false o baseUrl vacio, la consulta se omite.
 */
@Component
@ConfigurationProperties(prefix = "sunat")
public class SunatProperties {

    private boolean enabled = true;
    private String baseUrl = "";
    private String apiKey = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
