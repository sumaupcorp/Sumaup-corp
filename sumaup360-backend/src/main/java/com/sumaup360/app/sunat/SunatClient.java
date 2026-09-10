package com.sumaup360.app.sunat;

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

/**
 * Cliente HTTP del microservicio sumaup360-sunat. Consulta la Ficha RUC y devuelve
 * los campos normalizados. Best-effort: ante cualquier fallo devuelve Optional.empty().
 */
@Component
public class SunatClient {

    private static final Logger log = LoggerFactory.getLogger(SunatClient.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper;
    private final SunatProperties props;

    public SunatClient(ObjectMapper mapper, SunatProperties props) {
        this.mapper = mapper;
        this.props = props;
    }

    /** Datos relevantes de la Ficha RUC. Campos null si SUNAT no los provee. */
    public record RucData(
            String ruc,
            String razonSocial,
            String estado,
            String condicion,
            String tipoContribuyente,
            String actividadPrincipal,
            String ciiu,
            String fechaInscripcion,
            String fechaInicioActividades,
            String domicilioFiscal,
            String rawJson
    ) {
    }

    /** Resultado de la consulta: datos (si ok), status HTTP del microservicio y detalle. */
    public record RucResult(RucData data, int status, String detail) {
        public boolean ok() {
            return data != null;
        }

        /** 404 = el documento no tiene RUC registrado (caso valido de negocio). */
        public boolean notFound() {
            return status == 404;
        }
    }

    /** Resultado de validar la Clave SOL (login real en SUNAT). */
    public record SolValidation(boolean ok, String nombre, String detail, int status) {
    }

    /** Consulta por RUC. */
    public RucResult consultar(String ruc) {
        return call("/api/sunat/ruc/" + ruc, "RUC " + ruc);
    }

    /**
     * Valida la Clave SOL iniciando sesion en SUNAT (via microservicio). No persiste nada.
     * mode: "dni" (dni+clave) o "ruc" (ruc+usuario+clave).
     */
    public SolValidation validarSol(String mode, String dni, String ruc, String usuario, String clave) {
        if (!props.isEnabled() || props.getBaseUrl() == null || props.getBaseUrl().isBlank()) {
            return new SolValidation(false, null, "El servicio de validacion no esta disponible.", 0);
        }
        try {
            var body = mapper.createObjectNode();
            body.put("mode", mode);
            body.put("clave", clave == null ? "" : clave);
            if (dni != null) body.put("dni", dni);
            if (ruc != null) body.put("ruc", ruc);
            if (usuario != null) body.put("usuario", usuario);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl().replaceAll("/+$", "") + "/api/sunat/validar-sol"))
                    .header("X-API-Key", props.getApiKey() == null ? "" : props.getApiKey())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(90)) // el login con Playwright puede tardar
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode n = mapper.readTree(res.body());
            boolean ok = n.path("ok").asBoolean(false);
            String detail = text(n, "detail");
            return new SolValidation(ok, text(n, "nombre"), detail, res.statusCode());
        } catch (Exception e) {
            log.warn("Error validando Clave SOL: {}", e.getMessage());
            return new SolValidation(false, null, "No pudimos validar tu Clave SOL. Intenta de nuevo.", 0);
        }
    }

    /** Consulta por documento (DNI por defecto). docType: 1=DNI,4=CE,7=Pasaporte,A=Ced.Diplomatica. */
    public RucResult consultarPorDni(String dni, String docType) {
        String dt = (docType == null || docType.isBlank()) ? "1" : docType;
        return call("/api/sunat/dni/" + dni + "?doc_type=" + dt, "DNI " + dni);
    }

    private RucResult call(String path, String label) {
        if (!props.isEnabled() || props.getBaseUrl() == null || props.getBaseUrl().isBlank()) {
            log.debug("SUNAT client deshabilitado o sin baseUrl");
            return new RucResult(null, 0, null);
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl().replaceAll("/+$", "") + path))
                    .header("X-API-Key", props.getApiKey() == null ? "" : props.getApiKey())
                    .timeout(Duration.ofSeconds(60)) // el scraper puede tardar
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                log.warn("SUNAT respondio {} para {}", res.statusCode(), label);
                return new RucResult(null, res.statusCode(), detailOf(res.body()));
            }
            JsonNode n = mapper.readTree(res.body());
            JsonNode act = n.path("actividad_principal");
            JsonNode raw = n.path("raw"); // ficha completa de SUNAT
            String rawJson = raw.isMissingNode() ? res.body() : mapper.writeValueAsString(raw);
            RucData data = new RucData(
                    text(n, "ruc"),
                    text(n, "razon_social"),
                    text(n, "estado"),
                    text(n, "condicion"),
                    text(n, "tipo_contribuyente"),
                    text(act, "descripcion"),
                    text(act, "ciiu"),
                    text(raw, "fecha_inscripcion"),
                    text(raw, "fecha_inicio_actividades"),
                    text(n, "direccion"),
                    rawJson
            );
            return new RucResult(data, 200, null);
        } catch (Exception e) {
            log.warn("Error consultando SUNAT ({}): {}", label, e.getMessage());
            return new RucResult(null, 0, null);
        }
    }

    /** Extrae el campo "detail" del cuerpo de error del microservicio. */
    private String detailOf(String body) {
        try {
            return text(mapper.readTree(body), "detail");
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }
}
