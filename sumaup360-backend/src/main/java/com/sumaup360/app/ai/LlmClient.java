package com.sumaup360.app.ai;

import java.util.Optional;

/**
 * Abstraccion de un modelo de lenguaje (OpenAI, Gemini, ...). Best-effort: si no esta
 * configurado o falla, devuelve Optional.empty() y el que llama usa su fallback de reglas.
 */
public interface LlmClient {

    /** true si el cliente tiene credenciales y esta habilitado. */
    boolean isConfigured();

    /** Identificador del modelo (para auditoria/guardado del reporte). */
    String model();

    /** Genera con parametros por defecto (temperatura 0.4, 700 tokens). */
    default Optional<String> generate(String system, String user) {
        return generate(system, user, 0.4, 700);
    }

    /** Genera una respuesta. system define el rol; user es la consulta. Vacio si falla. */
    Optional<String> generate(String system, String user, double temperature, int maxTokens);
}
