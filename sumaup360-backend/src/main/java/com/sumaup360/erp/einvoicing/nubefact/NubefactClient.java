package com.sumaup360.erp.einvoicing.nubefact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP de NubeFact. Se le pasa la ruta (URL del emisor) y el token en cada
 * llamada, porque son POR EMPRESA (multi-tenant). NubeFact genera el XML, lo firma,
 * lo envia a SUNAT y devuelve el CDR, PDF, QR y hash.
 *
 * Autenticacion: header  Authorization: Token token="&lt;token&gt;".
 */
@Component
public class NubefactClient {

    private static final Logger log = LoggerFactory.getLogger(NubefactClient.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper;

    public NubefactClient(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** Resultado de emitir. ok=false trae el mensaje de error de NubeFact/SUNAT. */
    public record NubefactResult(
            boolean ok,
            String errors,
            boolean aceptadaPorSunat,
            String sunatDescription,
            String enlace,
            String enlacePdf,
            String enlaceXml,
            String enlaceCdr,
            String qr,
            String hash,
            Long numero,
            int httpStatus
    ) {
    }

    public NubefactResult emit(String ruta, String token, ObjectNode payload) {
        if (ruta == null || ruta.isBlank() || token == null || token.isBlank()) {
            return error("Faltan credenciales de NubeFact (ruta o token).", 0);
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ruta.trim()))
                    // NubeFact espera el token crudo en Authorization (no "Token token=...").
                    .header("Authorization", token.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode n = mapper.readTree(res.body());
            String errors = text(n, "errors");
            if (errors != null && !errors.isBlank()) {
                log.warn("NubeFact rechazo emision ({}): {}", res.statusCode(), errors);
                return error(errors, res.statusCode());
            }
            if (res.statusCode() >= 400) {
                return error("NubeFact respondio " + res.statusCode() + ".", res.statusCode());
            }
            return new NubefactResult(
                    true,
                    null,
                    n.path("aceptada_por_sunat").asBoolean(false),
                    text(n, "sunat_description"),
                    text(n, "enlace"),
                    text(n, "enlace_del_pdf"),
                    text(n, "enlace_del_xml"),
                    text(n, "enlace_del_cdr"),
                    text(n, "cadena_para_codigo_qr"),
                    text(n, "codigo_hash"),
                    n.path("numero").isNumber() ? n.path("numero").asLong() : null,
                    res.statusCode());
        } catch (Exception e) {
            log.warn("Error conectando con NubeFact: {}", e.getMessage());
            return error("No se pudo conectar con NubeFact: " + e.getMessage(), 0);
        }
    }

    private static NubefactResult error(String msg, int status) {
        return new NubefactResult(false, msg, false, null, null, null, null, null, null, null, null, status);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }
}
