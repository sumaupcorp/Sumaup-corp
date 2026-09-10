package com.sumaup360.app.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente de Google Gemini (generateContent). Best-effort: si no hay apiKey o falla,
 * devuelve Optional.empty() y el ChatbotService usa el generador de reglas como fallback.
 */
@Component
public class GeminiClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper;
    private final GeminiProperties props;

    public GeminiClient(ObjectMapper mapper, GeminiProperties props) {
        this.mapper = mapper;
        this.props = props;
    }

    @Override
    public boolean isConfigured() {
        return props.isEnabled() && props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    @Override
    public String model() {
        return props.getModel();
    }

    /** Genera una respuesta. systemInstruction define el rol; userText es la pregunta. */
    @Override
    public Optional<String> generate(String systemInstruction, String userText, double temperature, int maxTokens) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = Map.of(
                    "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", userText)))),
                    "generationConfig", Map.of("temperature", temperature, "maxOutputTokens", maxTokens)
            );
            String url = BASE + props.getModel() + ":generateContent?key=" + props.getApiKey();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("Gemini respondio {}: {}", res.statusCode(), trim(res.body()));
                return Optional.empty();
            }
            JsonNode text = mapper.readTree(res.body())
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode() || text.asText().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(text.asText().trim());
        } catch (Exception e) {
            log.warn("Error llamando a Gemini: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String trim(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
}
