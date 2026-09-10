package com.sumaup360.app.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Orientacion tributaria del onboarding (usuarios sin RUC). El usuario responde 5 preguntas
 * segun su actividad y recibe una recomendacion educativa de inscripcion/regimen (SUNAT)
 * con sustentacion tributaria real: por que le conviene, cuanto pagaria aprox., limites a
 * tener en cuenta, pasos para inscribirse y un plan B si su situacion cambia.
 */
public final class OrientationDtos {

    private OrientationDtos() {
    }

    /** Una pregunta del cuestionario con la respuesta elegida por el usuario. */
    public record OrientationAnswer(
            @NotBlank(message = "La pregunta es obligatoria.") String question,
            @NotBlank(message = "La respuesta es obligatoria.") String answer
    ) {
    }

    /** Si activity es null se usa el clientType del perfil. */
    public record AnalyzeRequest(
            String activity,
            @NotEmpty(message = "Las respuestas son obligatorias.") @Valid List<OrientationAnswer> answers
    ) {
    }

    /** Punto de sustentacion: conecta una respuesta del usuario con la regla tributaria concreta. */
    public record WhyPoint(
            String title,
            String detail
    ) {
    }

    /** Costo aproximado del regimen recomendado (valores referenciales). */
    public record EstimatedCost(
            String label,
            String amount,
            String note
    ) {
    }

    /** Plan B: regimen alternativo y cuando conviene evaluarlo. */
    public record Alternative(
            String regime,
            String when
    ) {
    }

    /**
     * Resultado de la orientacion con sustentacion tributaria real.
     * modelo: id del modelo de IA usado o "reglas" (fallback).
     */
    public record OrientationResult(
            String headline,
            String regime,
            String summary,
            List<WhyPoint> whyItFits,
            EstimatedCost estimatedCost,
            List<String> considerations,
            List<String> nextSteps,
            Alternative alternative,
            String disclaimer,
            String modelo
    ) {
    }

    /** Estado + resultado guardado de la orientacion de la persona. */
    public record OrientationStatusResponse(
            String status,
            JsonNode result
    ) {
    }
}
