package com.sumaup360.onboarding;

import com.sumaup360.app.sunat.SunatClient;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

/**
 * Consulta la Ficha RUC (microservicio sumaup360-sunat) durante el onboarding del SaaS y
 * sugiere el rubro por CIIU / actividad. No persiste nada: solo asiste al wizard.
 * El RUC es OPCIONAL en el onboarding; este lookup solo corre si el usuario lo ingresa.
 */
@Service
public class RucLookupService {

    /** CIIU rev.4 exacto -> rubro. */
    private static final Map<String, String> CIIU_TO_RUBRO = Map.ofEntries(
            Map.entry("5610", "restaurant"),
            Map.entry("4772", "pharmacy"),
            Map.entry("7500", "veterinary"),
            Map.entry("1071", "bakery"),
            Map.entry("4711", "minimarket"),
            Map.entry("4752", "hardware"),
            Map.entry("4761", "bookstore"),
            Map.entry("9602", "beauty"),
            Map.entry("9601", "laundry"),
            Map.entry("5510", "lodging"),   // alojamiento para estancias cortas
            Map.entry("5590", "lodging")    // otros tipos de alojamiento
    );

    /** Fallback por palabras clave en la actividad (SUNAT a veces devuelve CIIU rev.3). */
    private static final Map<String, String> KEYWORD_TO_RUBRO = Map.ofEntries(
            Map.entry("FARMAC", "pharmacy"),
            Map.entry("BOTIC", "pharmacy"),
            Map.entry("VETERINAR", "veterinary"),
            Map.entry("PANADER", "bakery"),
            Map.entry("PASTELER", "pastry"),
            Map.entry("RESTAURAN", "restaurant"),
            Map.entry("POLLER", "restaurant"),
            Map.entry("CEVICH", "restaurant"),
            Map.entry("CHIFA", "restaurant"),
            Map.entry("FERRETER", "hardware"),
            Map.entry("LIBRER", "bookstore"),
            Map.entry("PELUQUER", "beauty"),
            Map.entry("BELLEZA", "beauty"),
            Map.entry("LAVANDER", "laundry"),
            Map.entry("MINIMARKET", "minimarket"),
            Map.entry("ABARROTES", "minimarket"),
            Map.entry("HOTEL", "lodging"),
            Map.entry("HOSTAL", "lodging"),
            Map.entry("HOSPEDAJE", "lodging"),
            Map.entry("ALOJAMIENTO", "lodging"),
            Map.entry("ALBERGUE", "lodging")
    );

    private final SunatClient sunatClient;

    public RucLookupService(SunatClient sunatClient) {
        this.sunatClient = sunatClient;
    }

    public record RucLookupResponse(
            boolean found,
            String ruc,
            String razonSocial,
            String estado,
            String condicion,
            String tipoContribuyente,
            String actividadPrincipal,
            String ciiu,
            String domicilioFiscal,
            String suggestedBusinessTypeCode,
            String detail
    ) {
        static RucLookupResponse notFound(String detail) {
            return new RucLookupResponse(false, null, null, null, null, null, null, null, null, null, detail);
        }
    }

    public RucLookupResponse lookup(String ruc) {
        String clean = ruc == null ? "" : ruc.trim();
        if (!clean.matches("(10|15|16|17|20)\\d{9}")) {
            return RucLookupResponse.notFound("El RUC debe tener 11 digitos validos.");
        }
        SunatClient.RucResult result = sunatClient.consultar(clean);
        if (!result.ok()) {
            String detail = result.notFound()
                    ? (result.detail() != null ? result.detail() : "El RUC no figura en SUNAT.")
                    : "No pudimos consultar SUNAT en este momento; completa los datos manualmente.";
            return RucLookupResponse.notFound(detail);
        }
        SunatClient.RucData d = result.data();
        return new RucLookupResponse(true, d.ruc(), d.razonSocial(), d.estado(), d.condicion(),
                d.tipoContribuyente(), d.actividadPrincipal(), d.ciiu(), d.domicilioFiscal(),
                suggestRubro(d.ciiu(), d.actividadPrincipal()), null);
    }

    /** Sugerencia no vinculante: el usuario siempre confirma el rubro en el wizard. */
    String suggestRubro(String ciiu, String actividad) {
        if (ciiu != null && CIIU_TO_RUBRO.containsKey(ciiu.trim())) {
            return CIIU_TO_RUBRO.get(ciiu.trim());
        }
        if (actividad != null) {
            String upper = actividad.toUpperCase(Locale.ROOT);
            for (Map.Entry<String, String> e : KEYWORD_TO_RUBRO.entrySet()) {
                if (upper.contains(e.getKey())) {
                    return e.getValue();
                }
            }
        }
        return null;
    }
}
