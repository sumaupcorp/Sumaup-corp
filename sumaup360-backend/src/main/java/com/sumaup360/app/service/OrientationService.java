package com.sumaup360.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sumaup360.app.ai.LlmClient;
import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.dto.OrientationDtos.Alternative;
import com.sumaup360.app.dto.OrientationDtos.AnalyzeRequest;
import com.sumaup360.app.dto.OrientationDtos.EstimatedCost;
import com.sumaup360.app.dto.OrientationDtos.OrientationAnswer;
import com.sumaup360.app.dto.OrientationDtos.OrientationResult;
import com.sumaup360.app.dto.OrientationDtos.OrientationStatusResponse;
import com.sumaup360.app.dto.OrientationDtos.WhyPoint;
import com.sumaup360.app.enums.ClientType;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.common.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orientacion tributaria del onboarding (Linea Personas, usuarios SIN RUC). El usuario
 * responde 5 preguntas segun su actividad (taxista, repartidor, profesional) y la IA
 * genera una recomendacion educativa con sustentacion tributaria real: como inscribirse
 * ante SUNAT, regimen sugerido (Nuevo RUS con categoria, Regimen MYPE Tributario o rentas
 * de 4ta categoria), por que le conviene segun sus respuestas, costo aproximado, limites,
 * pasos concretos y un plan B. Si la IA falla o devuelve JSON invalido, se usa un fallback
 * determinista de reglas con los mismos datos referenciales.
 */
@Service
public class OrientationService {

    private static final Logger log = LoggerFactory.getLogger(OrientationService.class);

    /** Aviso legal estandar del resultado (valores referenciales, no asesoria vinculante). */
    private static final String DISCLAIMER =
            "Orientación educativa con valores referenciales; no reemplaza la asesoría de un "
                    + "contador. Verifica los montos vigentes en SUNAT.";

    private final PersonProfileRepository profileRepository;
    private final LlmClient llm;
    private final ObjectMapper objectMapper;

    public OrientationService(PersonProfileRepository profileRepository, LlmClient llm,
                              ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.llm = llm;
        this.objectMapper = objectMapper;
    }

    /** Analiza las respuestas (IA con fallback de reglas) y guarda el resultado en el perfil. */
    @Transactional
    public OrientationResult analyze(UUID userId, AnalyzeRequest req) {
        PersonProfile profile = profileRepository.findByUserId(userId).orElseGet(() -> {
            PersonProfile n = new PersonProfile();
            n.setUserId(userId);
            return n;
        });

        // Un solo intento por cuenta: si ya la completo, no se analiza de nuevo ni se
        // sobrescribe el resultado guardado. Soporte puede reactivarla desde el backoffice.
        if ("COMPLETED".equals(profile.getOrientationStatus())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Ya utilizaste tu orientación tributaria. Solicita a soporte reactivarla.");
        }

        ClientType activity = resolveActivity(req.activity(), profile);
        List<OrientationAnswer> answers = req.answers();

        OrientationResult result = askAi(activity, answers)
                .orElseGet(() -> fallback(activity, answers));

        // Persistencia: guarda el JSON del resultado y marca la orientacion como completada.
        profile.setOrientationResult(toJson(result));
        profile.setOrientationStatus("COMPLETED");
        profileRepository.save(profile);
        return result;
    }

    /** Estado y resultado guardado de la orientacion de la persona. */
    @Transactional(readOnly = true)
    public OrientationStatusResponse status(UUID userId) {
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        if (p == null) {
            return new OrientationStatusResponse(null, null);
        }
        JsonNode result = null;
        if (p.getOrientationResult() != null && !p.getOrientationResult().isBlank()) {
            try {
                result = objectMapper.readTree(p.getOrientationResult());
            } catch (Exception e) {
                log.warn("orientation_result invalido para el usuario {}", userId);
            }
        }
        return new OrientationStatusResponse(p.getOrientationStatus(), result);
    }

    // --- IA ---

    private Optional<OrientationResult> askAi(ClientType activity, List<OrientationAnswer> answers) {
        String system = """
                Eres un orientador tributario peruano de SUNAT para personas que AUN NO tienen RUC.
                Tu orientacion es educativa (no es asesoria legal vinculante). Usa SOLO estos datos
                referenciales del sistema tributario peruano (presentalos como aproximados donde
                corresponda):
                - Nuevo RUS (solo persona natural): Categoria 1 = ingresos y compras hasta S/ 5,000
                  al mes, cuota fija S/ 20 al mes; Categoria 2 = hasta S/ 8,000 al mes, cuota fija
                  S/ 50 al mes; tope anual S/ 96,000. SOLO emite boletas de venta (NO facturas).
                  La cuota fija reemplaza al IGV y al Impuesto a la Renta.
                - Regimen MYPE Tributario (RMT): puede emitir facturas y boletas; pago a cuenta
                  mensual del 1% de los ingresos netos (mientras no supere 300 UIT); renta anual
                  10% hasta 15 UIT de utilidad y 29.5% por el exceso; ingresos netos hasta 1,700
                  UIT; permite deducir gastos sustentados con comprobantes (combustible,
                  mantenimiento, comisiones de apps, alquiler de vehiculo, etc.).
                - Rentas de 4ta categoria (servicios profesionales independientes): recibos por
                  honorarios electronicos; retencion del 8% cuando el pagador es agente de
                  retencion y el recibo supera S/ 1,500; deduccion automatica del 20% de la renta
                  bruta mas 7 UIT al ano; puede pedir SUSPENSION de retenciones ante SUNAT si
                  proyecta ingresos anuales por debajo del umbral vigente (aprox. S/ 48,000
                  referencial); no paga IGV por honorarios.
                - UIT referencial 2026: S/ 5,350.
                Instrucciones de la recomendacion:
                1) Elige como inscribirse (headline, ej. "Persona Natural con Negocio" o
                   "Persona Natural con RUC") y el regimen (regime). Si recomiendas el Nuevo RUS,
                   incluye la categoria segun el rango de ingresos declarado, ej.
                   "Nuevo RUS - Categoría 1".
                2) whyItFits: 3 a 5 puntos de SUSTENTACION REAL. Cada punto debe conectar una
                   respuesta concreta del usuario con la regla tributaria concreta (montos, tasas,
                   limites). Nada de repetir sus respuestas sin explicar la regla.
                3) estimatedCost coherente con el regimen: Nuevo RUS = cuota fija "S/ 20.00" o
                   "S/ 50.00" segun categoria; RMT = "aprox. 1% de tus ingresos mensuales" con un
                   ejemplo calculado usando el punto medio del rango de ingresos declarado; 4ta =
                   "retención 8% solo en recibos mayores a S/ 1,500" o "S/ 0 si logras la
                   suspensión" cuando los ingresos proyectados esten por debajo del umbral.
                4) considerations: 2 a 4 limites o alertas reales del regimen (topes, migracion,
                   que NO puede hacer).
                5) nextSteps: 3 a 4 pasos concretos para inscribirse (RUC gratis en SUNAT Virtual
                   con DNI y Clave SOL, activar emision electronica, etc.).
                6) alternative: el plan B (otro regimen) y cuando conviene evaluarlo.
                Responde SOLO un JSON valido, sin texto adicional ni bloques de codigo, con
                exactamente estas claves:
                {"headline": "como inscribirse",
                "regime": "regimen sugerido (con categoria si aplica)",
                "summary": "una frase clara de la recomendacion, en segunda persona",
                "whyItFits": [{"title": "titulo corto", "detail": "sustentacion que une su respuesta con la regla tributaria"}],
                "estimatedCost": {"label": "nombre del pago", "amount": "monto o formula", "note": "explicacion corta del calculo"},
                "considerations": ["limite o alerta real"],
                "nextSteps": ["paso concreto para inscribirse"],
                "alternative": {"regime": "regimen plan B", "when": "cuando evaluarlo"},
                "disclaimer": "aviso de orientacion educativa con valores referenciales"}
                Todo en espanol de Peru, tono cercano y claro.
                """;
        StringBuilder user = new StringBuilder();
        user.append("Actividad del usuario: ").append(activityLabel(activity)).append("\n");
        user.append("Respuestas del cuestionario de orientacion:\n");
        for (OrientationAnswer a : answers) {
            user.append("- ").append(a.question().trim()).append(" -> ").append(a.answer().trim()).append("\n");
        }
        Optional<String> raw = llm.generate(system, user.toString(), 0.3, 1200);
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(extractJson(raw.get()));
            String headline = node.path("headline").asText(null);
            String regime = node.path("regime").asText(null);
            String summary = node.path("summary").asText(null);
            if (isBlank(headline) || isBlank(regime) || isBlank(summary)) {
                return Optional.empty();
            }

            List<WhyPoint> whyItFits = new ArrayList<>();
            for (JsonNode w : node.path("whyItFits")) {
                String title = w.path("title").asText(null);
                String detail = w.path("detail").asText(null);
                if (!isBlank(title) && !isBlank(detail) && whyItFits.size() < 5) {
                    whyItFits.add(new WhyPoint(title.trim(), detail.trim()));
                }
            }

            JsonNode cost = node.path("estimatedCost");
            String costLabel = cost.path("label").asText(null);
            String costAmount = cost.path("amount").asText(null);
            String costNote = cost.path("note").asText(null);

            List<String> considerations = readStrings(node.path("considerations"), 4);
            List<String> nextSteps = readStrings(node.path("nextSteps"), 4);

            JsonNode alt = node.path("alternative");
            String altRegime = alt.path("regime").asText(null);
            String altWhen = alt.path("when").asText(null);

            if (whyItFits.isEmpty() || isBlank(costLabel) || isBlank(costAmount)
                    || considerations.isEmpty() || nextSteps.isEmpty()
                    || isBlank(altRegime) || isBlank(altWhen)) {
                return Optional.empty();
            }

            String disclaimer = node.path("disclaimer").asText(null);
            if (isBlank(disclaimer)) {
                disclaimer = DISCLAIMER;
            }

            return Optional.of(new OrientationResult(
                    headline.trim(), regime.trim(), summary.trim(), whyItFits,
                    new EstimatedCost(costLabel.trim(), costAmount.trim(),
                            isBlank(costNote) ? null : costNote.trim()),
                    considerations, nextSteps,
                    new Alternative(altRegime.trim(), altWhen.trim()),
                    disclaimer.trim(), llm.model()));
        } catch (Exception e) {
            log.warn("La IA devolvio JSON invalido para la orientacion; se usa el fallback de reglas.");
            return Optional.empty();
        }
    }

    /** Lee un array de textos no vacios hasta un maximo. */
    private static List<String> readStrings(JsonNode array, int max) {
        List<String> out = new ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode n : array) {
                String v = n.asText(null);
                if (!isBlank(v) && out.size() < max) {
                    out.add(v.trim());
                }
            }
        }
        return out;
    }

    /** Recorta ruido tipico del modelo (bloques ```json ... ```) y se queda con el objeto JSON. */
    private static String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    // --- Fallback determinista de reglas (mismos datos referenciales que el prompt) ---

    private OrientationResult fallback(ClientType activity, List<OrientationAnswer> answers) {
        if (activity == ClientType.SERVICIOS_PROFESIONALES) {
            return fallbackCuartaCategoria(answers);
        }
        boolean necesitaFactura = anyAnswerContains(answers, "factura", "ambos");
        boolean ingresosMasDe8000 = declaraMasDe8000(answers);
        if (necesitaFactura || ingresosMasDe8000) {
            return fallbackMype(activity, answers, necesitaFactura, ingresosMasDe8000);
        }
        return fallbackNuevoRus(activity, answers);
    }

    /** Profesional independiente: rentas de 4ta categoria (recibos por honorarios). */
    private OrientationResult fallbackCuartaCategoria(List<OrientationAnswer> answers) {
        boolean ingresosBajos = !declaraMasDe8000(answers) && !declaraEntre5000y8000(answers);
        boolean gastosFrecuentes = declaraGastosFrecuentes(answers);

        List<WhyPoint> why = new ArrayList<>();
        why.add(new WhyPoint("Prestas servicios profesionales independientes",
                "Indicaste que trabajas por tu cuenta ofreciendo servicios: eso califica como "
                        + "rentas de 4ta categoría y se sustenta con recibos por honorarios electrónicos."));
        why.add(new WhyPoint("No pagas IGV por tus honorarios",
                "Los recibos por honorarios no llevan IGV, así que no le cargas impuesto a tus "
                        + "clientes ni presentas declaraciones de IGV por tus servicios."));
        why.add(new WhyPoint("Deducciones automáticas a tu favor",
                "SUNAT te descuenta automáticamente el 20% de tu renta bruta más 7 UIT al año "
                        + "(aprox. S/ 37,450 con la UIT referencial 2026 de S/ 5,350), por lo que con "
                        + "ingresos moderados tu renta anual suele ser baja o cero."));
        if (ingresosBajos) {
            why.add(new WhyPoint("Puedes trabajar sin retenciones",
                    "Con los ingresos que declaraste, si proyectas menos de aprox. S/ 48,000 al año "
                            + "puedes pedir a SUNAT la suspensión de retenciones y cobrar tus honorarios completos."));
        } else {
            why.add(new WhyPoint("La retención del 8% es solo un adelanto",
                    "Te retienen 8% únicamente en recibos mayores a S/ 1,500 y ese monto se descuenta "
                            + "como pago a cuenta de tu renta anual."));
        }

        EstimatedCost cost;
        if (ingresosBajos) {
            cost = new EstimatedCost("Retención de 4ta categoría", "S/ 0 si logras la suspensión",
                    "Si proyectas ingresos anuales por debajo de aprox. S/ 48,000 puedes pedir la "
                            + "suspensión de retenciones; sin suspensión, te retienen 8% solo en recibos "
                            + "mayores a S/ 1,500.");
        } else {
            cost = new EstimatedCost("Retención de 4ta categoría",
                    "8% solo en recibos mayores a S/ 1,500",
                    "Es un pago a cuenta de tu renta anual; además tienes deducción automática del "
                            + "20% de tu renta bruta más 7 UIT al año.");
        }

        List<String> considerations = new ArrayList<>();
        considerations.add("La retención del 8% aplica cuando quien te paga es agente de retención "
                + "y el recibo supera S/ 1,500.");
        considerations.add("Si además vendes productos o pones un negocio (actividad empresarial), "
                + "esas rentas ya no son de 4ta categoría: para eso evalúa el Régimen MYPE Tributario.");
        if (gastosFrecuentes) {
            considerations.add("Mencionaste gastos frecuentes: en 4ta categoría no se deducen gastos "
                    + "reales (la deducción es automática); si tu actividad se vuelve empresarial con "
                    + "muchos gastos, el RMT permite deducirlos con comprobantes.");
        }
        considerations.add("Verifica cada año en SUNAT el umbral vigente para la suspensión de "
                + "retenciones (aprox. S/ 48,000 referencial).");

        List<String> nextSteps = List.of(
                "Obtén tu RUC gratis en SUNAT Virtual (www.sunat.gob.pe) con tu DNI y Clave SOL, "
                        + "como Persona Natural con rentas de 4ta categoría; queda activo el mismo día.",
                "Activa la emisión de recibos por honorarios electrónicos en SUNAT Operaciones en Línea.",
                "Si proyectas ingresos anuales por debajo de aprox. S/ 48,000, presenta la solicitud "
                        + "de suspensión de retenciones (Formulario Virtual 1609).",
                "Emite un recibo por honorarios por cada servicio y revisa al cierre del año si te "
                        + "corresponde declaración anual.");

        return new OrientationResult(
                "Persona Natural con RUC",
                "Rentas de 4ta categoría (Recibos por Honorarios)",
                "Según tus respuestas, te conviene inscribirte como Persona Natural con RUC y emitir "
                        + "recibos por honorarios electrónicos (rentas de 4ta categoría).",
                capWhy(why),
                cost,
                capStrings(considerations, 4),
                nextSteps,
                new Alternative("Régimen MYPE Tributario",
                        "Si más adelante desarrollas una actividad empresarial (vendes productos, montas "
                                + "un negocio con gastos propios) o tus clientes necesitan facturas de negocio."),
                DISCLAIMER, "reglas");
    }

    /** Actividad empresarial que necesita factura o supera el tope del Nuevo RUS: RMT. */
    private OrientationResult fallbackMype(ClientType activity, List<OrientationAnswer> answers,
                                           boolean necesitaFactura, boolean ingresosMasDe8000) {
        boolean gastosFrecuentes = declaraGastosFrecuentes(answers);
        int puntoMedio = puntoMedioMensual(answers);

        List<WhyPoint> why = new ArrayList<>();
        if (necesitaFactura) {
            why.add(new WhyPoint("Puedes emitir facturas y boletas",
                    "Indicaste que tus clientes te piden factura: el Régimen MYPE Tributario permite "
                            + "emitir facturas y boletas, mientras que el Nuevo RUS solo permite boletas de venta."));
        }
        if (ingresosMasDe8000) {
            why.add(new WhyPoint("Tus ingresos superan el tope del Nuevo RUS",
                    "Declaraste más de S/ 8,000 al mes y el Nuevo RUS solo llega a S/ 8,000 mensuales "
                            + "(S/ 96,000 al año), así que el RMT es la opción natural para tu nivel de ingresos."));
        }
        if (gastosFrecuentes) {
            why.add(new WhyPoint("Puedes deducir tus gastos",
                    "Mencionaste gastos frecuentes: en el RMT el combustible, mantenimiento, comisiones "
                            + "de apps o alquiler de vehículo se deducen si están sustentados con comprobantes, "
                            + "y reducen tu impuesto anual."));
        }
        why.add(new WhyPoint("Pago mensual bajo mientras creces",
                "En el RMT pagas a cuenta solo el 1% de tus ingresos netos mensuales mientras no "
                        + "superes 300 UIT de ingresos en el año."));
        why.add(new WhyPoint("Tasa anual reducida para MYPE",
                "En la declaración anual pagas 10% por las primeras 15 UIT de utilidad (aprox. "
                        + "S/ 80,250 con la UIT referencial 2026 de S/ 5,350) y 29.5% solo por el exceso."));

        String ejemplo = puntoMedio > 0
                ? String.format(java.util.Locale.US,
                "Ejemplo: con ingresos de S/ %,d al mes pagarías aprox. S/ %,d de pago a cuenta. ",
                puntoMedio, Math.round(puntoMedio / 100.0))
                : "";
        EstimatedCost cost = new EstimatedCost("Pago a cuenta mensual",
                "Aprox. 1% de tus ingresos mensuales",
                ejemplo + "Al cierre del año regularizas: 10% hasta 15 UIT de utilidad y 29.5% por el exceso.");

        List<String> considerations = new ArrayList<>();
        considerations.add("El 1% mensual es un pago a cuenta: al año regularizas tu renta con 10% "
                + "hasta 15 UIT de utilidad y 29.5% por el exceso.");
        considerations.add("Solo los gastos sustentados con comprobantes se deducen: guarda facturas de "
                + "combustible, mantenimiento, comisiones de apps o alquiler de vehículo.");
        considerations.add("Declaras cada mes según el cronograma de SUNAT, incluso en meses sin ingresos.");
        considerations.add("El RMT admite ingresos netos de hasta 1,700 UIT al año; si los superas, "
                + "pasarías al Régimen General.");

        List<String> nextSteps = List.of(
                "Obtén tu RUC gratis en SUNAT Virtual (www.sunat.gob.pe) con tu DNI y Clave SOL; "
                        + "queda activo el mismo día.",
                "Al inscribirte, elige el Régimen MYPE Tributario e indica tu actividad económica ("
                        + activityLabel(activity).toLowerCase() + ").",
                "Activa la emisión de facturas y boletas electrónicas (emisor gratuito de SUNAT u otro autorizado).",
                "Exige y guarda comprobantes por tus gastos del negocio para deducirlos en tu declaración.");

        Alternative alternative;
        if (ingresosMasDe8000) {
            alternative = new Alternative("Régimen General",
                    "Si tus ingresos netos superan las 1,700 UIT al año o tu negocio necesita una "
                            + "estructura mayor, evalúalo con un contador.");
        } else {
            alternative = new Alternative("Nuevo RUS",
                    "Si tus ingresos se mantienen por debajo de S/ 8,000 al mes y dejas de necesitar "
                            + "facturas, el Nuevo RUS es más simple y de cuota fija.");
        }

        return new OrientationResult(
                "Persona Natural con Negocio",
                "Régimen MYPE Tributario",
                "Según tus respuestas, te conviene inscribirte como Persona Natural con Negocio en el "
                        + "Régimen MYPE Tributario.",
                capWhy(why),
                cost,
                capStrings(considerations, 4),
                nextSteps,
                alternative,
                DISCLAIMER, "reglas");
    }

    /** Ingresos dentro del tope y solo boletas: Nuevo RUS con categoria segun el rango declarado. */
    private OrientationResult fallbackNuevoRus(ClientType activity, List<OrientationAnswer> answers) {
        boolean categoria2 = declaraEntre5000y8000(answers) || declaraHasta8000(answers);
        boolean gastosFrecuentes = declaraGastosFrecuentes(answers);
        int categoria = categoria2 ? 2 : 1;
        String cuota = categoria2 ? "S/ 50.00" : "S/ 20.00";
        String topeCategoria = categoria2 ? "S/ 8,000" : "S/ 5,000";

        List<WhyPoint> why = new ArrayList<>();
        why.add(new WhyPoint("Solo necesitas emitir boletas",
                "Indicaste que tus clientes son personas y te basta con boletas de venta: el Nuevo RUS "
                        + "emite solo boletas y su cuota fija reemplaza al IGV y al Impuesto a la Renta."));
        why.add(new WhyPoint("Tus ingresos encajan en la Categoría " + categoria,
                "Declaraste ingresos de hasta " + topeCategoria + " al mes: eso corresponde a la "
                        + "Categoría " + categoria + " del Nuevo RUS, con cuota fija de " + cuota + " mensuales."));
        why.add(new WhyPoint("Tributación simple, sin contabilidad completa",
                "Pagas una sola cuota fija al mes, no llevas libros contables completos ni presentas "
                        + "declaración anual de renta."));
        why.add(new WhyPoint("Es el régimen típico para tu actividad",
                "Como " + activityLabel(activity).toLowerCase() + " que trabaja como persona natural "
                        + "con ingresos dentro del tope, el Nuevo RUS es la puerta de entrada más simple "
                        + "a la formalidad."));

        EstimatedCost cost = new EstimatedCost("Cuota fija mensual", cuota,
                "Categoría " + categoria + " del Nuevo RUS: ingresos hasta " + topeCategoria
                        + " al mes. Reemplaza IGV e Impuesto a la Renta.");

        List<String> considerations = new ArrayList<>();
        considerations.add("Si superas S/ 8,000 de ingresos al mes (o S/ 96,000 al año) deberás migrar "
                + "a otro régimen.");
        considerations.add("En el Nuevo RUS no puedes emitir facturas; si una empresa te la pide, "
                + "evalúa el Régimen MYPE Tributario.");
        if (!categoria2) {
            considerations.add("Si un mes superas S/ 5,000 de ingresos o compras pasas a la Categoría 2 "
                    + "y la cuota sube a S/ 50.00.");
        }
        if (gastosFrecuentes) {
            considerations.add("Mencionaste gastos frecuentes (combustible, mantenimiento): en el Nuevo "
                    + "RUS no se deducen; si crecen, el Régimen MYPE Tributario permite deducirlos con comprobantes.");
        }

        List<String> nextSteps = List.of(
                "Obtén tu RUC gratis en SUNAT Virtual (www.sunat.gob.pe) con tu DNI y Clave SOL; "
                        + "queda activo el mismo día.",
                "Al inscribirte, elige el Nuevo RUS como régimen e indica tu actividad económica ("
                        + activityLabel(activity).toLowerCase() + ").",
                "Paga tu cuota fija de " + cuota + " cada mes según el cronograma de SUNAT (banca "
                        + "móvil, agentes o app de SUNAT).",
                "Emite boletas de venta desde el emisor gratuito de SUNAT o la app de SUNAT.");

        return new OrientationResult(
                "Persona Natural con Negocio",
                "Nuevo RUS - Categoría " + categoria,
                "Según tus respuestas, te conviene inscribirte como Persona Natural con Negocio en el "
                        + "Nuevo RUS, Categoría " + categoria + ".",
                capWhy(why),
                cost,
                capStrings(considerations, 4),
                nextSteps,
                new Alternative("Régimen MYPE Tributario",
                        "Si más adelante necesitas emitir facturas a empresas o superas los límites del "
                                + "Nuevo RUS (S/ 8,000 al mes o S/ 96,000 al año)."),
                DISCLAIMER, "reglas");
    }

    // --- Deteccion de respuestas (rangos de ingresos, factura, gastos) ---

    /** Declaro ingresos por encima de S/ 8,000 mensuales (fuera del Nuevo RUS). */
    private static boolean declaraMasDe8000(List<OrientationAnswer> answers) {
        for (OrientationAnswer a : answers) {
            String flat = flatten(a.answer());
            if (flat.contains("8000") && (flat.contains("mas de") || flat.contains("mayor a")
                    || flat.contains("mayores a") || flat.contains("superior"))) {
                return true;
            }
        }
        return false;
    }

    /** Declaro un rango entre S/ 5,000 y S/ 8,000 mensuales (Categoria 2 del Nuevo RUS). */
    private static boolean declaraEntre5000y8000(List<OrientationAnswer> answers) {
        for (OrientationAnswer a : answers) {
            String flat = flatten(a.answer());
            if (flat.contains("5000") && flat.contains("8000")) {
                return true;
            }
        }
        return false;
    }

    /** Declaro "hasta S/ 8,000" sin mencionar los S/ 5,000 (se asume Categoria 2). */
    private static boolean declaraHasta8000(List<OrientationAnswer> answers) {
        for (OrientationAnswer a : answers) {
            String flat = flatten(a.answer());
            if (flat.contains("8000") && !flat.contains("5000") && !flat.contains("mas de")
                    && !flat.contains("mayor a") && !flat.contains("superior")) {
                return true;
            }
        }
        return false;
    }

    private static boolean declaraGastosFrecuentes(List<OrientationAnswer> answers) {
        return anyAnswerContains(answers, "gasto", "combustible", "gasolina", "mantenimiento",
                "repuesto", "comision", "alquiler");
    }

    /**
     * Punto medio mensual referencial del rango de ingresos declarado, para el ejemplo del 1%
     * del RMT. 0 si no se puede estimar.
     */
    private static int puntoMedioMensual(List<OrientationAnswer> answers) {
        if (declaraMasDe8000(answers)) {
            return 10_000;
        }
        if (declaraEntre5000y8000(answers)) {
            return 6_500;
        }
        if (declaraHasta8000(answers)) {
            return 4_000;
        }
        for (OrientationAnswer a : answers) {
            if (flatten(a.answer()).contains("5000")) {
                return 2_500;
            }
        }
        return 0;
    }

    private static boolean anyAnswerContains(List<OrientationAnswer> answers, String... needles) {
        for (OrientationAnswer a : answers) {
            String normalized = normalize(a.answer());
            for (String needle : needles) {
                if (normalized.contains(normalize(needle))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Minusculas y sin tildes, para comparar respuestas escritas de cualquier forma. */
    private static String normalize(String s) {
        String lower = s == null ? "" : s.toLowerCase();
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Como normalize, pero ademas sin separadores de miles para comparar montos (S/ 8,000 -> 8000). */
    private static String flatten(String s) {
        return normalize(s).replace(",", "").replace(".", "");
    }

    private static List<WhyPoint> capWhy(List<WhyPoint> points) {
        return points.size() <= 5 ? points : new ArrayList<>(points.subList(0, 5));
    }

    private static List<String> capStrings(List<String> items, int max) {
        return items.size() <= max ? items : new ArrayList<>(items.subList(0, max));
    }

    // --- Utilitarios ---

    private ClientType resolveActivity(String requested, PersonProfile profile) {
        if (requested != null && !requested.isBlank()) {
            try {
                return ClientType.valueOf(requested.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Actividad invalida: usa TAXISTA, DELIVERY_PEYA o SERVICIOS_PROFESIONALES.");
            }
        }
        if (profile.getClientType() != null) {
            return profile.getClientType();
        }
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Indica tu actividad (TAXISTA, DELIVERY_PEYA o SERVICIOS_PROFESIONALES) o completa tu perfil.");
    }

    private static String activityLabel(ClientType activity) {
        return switch (activity) {
            case TAXISTA -> "Taxista";
            case DELIVERY_PEYA -> "Repartidor de delivery";
            case SERVICIOS_PROFESIONALES -> "Profesional independiente (servicios profesionales)";
        };
    }

    private String toJson(OrientationResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar el resultado de la orientacion.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
