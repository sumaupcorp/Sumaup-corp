package com.sumaup360.app.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Selector de proveedor de IA. `ai.provider` = openai | gemini (por defecto openai). Si el
 * proveedor primario no esta configurado o no responde, intenta con el otro (respaldo). Es el
 * bean {@link LlmClient} primario que inyectan los servicios (chat, diagnostico).
 */
@Primary
@Component
public class LlmRouter implements LlmClient {

    private final OpenAiClient openAi;
    private final GeminiClient gemini;
    private final String provider;

    public LlmRouter(OpenAiClient openAi, GeminiClient gemini,
                     @Value("${ai.provider:openai}") String provider) {
        this.openAi = openAi;
        this.gemini = gemini;
        this.provider = provider == null ? "openai" : provider.trim().toLowerCase();
    }

    private LlmClient primary() {
        return "gemini".equals(provider) ? gemini : openAi;
    }

    private LlmClient secondary() {
        return "gemini".equals(provider) ? openAi : gemini;
    }

    @Override
    public boolean isConfigured() {
        return primary().isConfigured() || secondary().isConfigured();
    }

    @Override
    public String model() {
        return primary().isConfigured() ? primary().model() : secondary().model();
    }

    @Override
    public Optional<String> generate(String system, String user, double temperature, int maxTokens) {
        Optional<String> r = primary().generate(system, user, temperature, maxTokens);
        if (r.isPresent()) {
            return r;
        }
        // Respaldo: si el primario no respondio (sin cuota/no configurado), prueba el otro.
        return secondary().generate(system, user, temperature, maxTokens);
    }
}
