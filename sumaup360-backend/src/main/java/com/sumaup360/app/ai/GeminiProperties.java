package com.sumaup360.app.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuracion del modelo Gemini (prefijo `gemini`). Si apiKey vacio, se usa el fallback. */
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private boolean enabled = true;
    private String apiKey = "";
    private String model = "gemini-2.0-flash";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
