package com.sumaup360.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Alta de cuentas en Firebase via REST (Identity Toolkit, Web API key). Compartido por el
 * Backoffice (staff) y el SaaS (trabajadores de tenant). No usa el Admin SDK porque el
 * OAuth del service account es fragil en este entorno; el backend sigue siendo la
 * autoridad de RBAC — Firebase solo guarda la identidad.
 */
@Service
public class FirebaseAccountService {

    private final ObjectMapper objectMapper;
    private final String webApiKey;
    private final HttpClient http = HttpClient.newHttpClient();

    public FirebaseAccountService(ObjectMapper objectMapper,
                                  @Value("${firebase.web-api-key:}") String webApiKey) {
        this.objectMapper = objectMapper;
        this.webApiKey = webApiKey;
    }

    /** Crea el usuario (correo+contrasena) y devuelve su uid (localId). */
    public String createUser(String email, String password) {
        if (webApiKey == null || webApiKey.isBlank()) {
            throw new BadRequestException("Falta firebase.web-api-key para crear cuentas.");
        }
        try {
            String body = objectMapper.writeValueAsString(
                    Map.of("email", email, "password", password, "returnSecureToken", true));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + webApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(res.body());
            if (res.statusCode() >= 400) {
                String msg = json.path("error").path("message").asText("error");
                if ("EMAIL_EXISTS".equals(msg)) {
                    throw new ConflictException("Ese correo ya tiene una cuenta.");
                }
                if (msg.startsWith("WEAK_PASSWORD")) {
                    throw new BadRequestException("La contrasena debe tener al menos 6 caracteres.");
                }
                throw new BadRequestException("No se pudo crear la cuenta: " + msg);
            }
            return json.path("localId").asText();
        } catch (ConflictException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Error creando la cuenta: " + e.getMessage());
        }
    }
}
