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
 * Cliente de OpenAI (Chat Completions). Best-effort: si no hay apiKey o falla, devuelve
 * Optional.empty() y el que llama usa su fallback (o el router prueba otro proveedor).
 */
@Component
public class OpenAiClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper;
    private final OpenAiProperties props;

    public OpenAiClient(ObjectMapper mapper, OpenAiProperties props) {
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

    @Override
    public Optional<String> generate(String system, String user, double temperature, int maxTokens) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", props.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)),
                    "temperature", temperature,
                    "max_tokens", maxTokens
            );
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .timeout(Duration.ofSeconds(40))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("OpenAI respondio {}: {}", res.statusCode(), trim(res.body()));
                return Optional.empty();
            }
            JsonNode text = mapper.readTree(res.body())
                    .path("choices").path(0).path("message").path("content");
            if (text.isMissingNode() || text.asText().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(text.asText().trim());
        } catch (Exception e) {
            log.warn("Error llamando a OpenAI: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String trim(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
}
