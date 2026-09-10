package com.sumaup360.app.ai;

import com.sumaup360.app.enums.ClientType;
import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Configuracion editable del "cerebro" de la IA (system prompts, contexto por caso, parametros). */
@Entity
@Table(name = "ai_prompt", schema = "app")
@Getter
@Setter
public class AiPrompt extends BaseEntity {

    @Column(name = "chat_system", columnDefinition = "text", nullable = false)
    private String chatSystem = "";

    @Column(name = "diagnosis_system", columnDefinition = "text", nullable = false)
    private String diagnosisSystem = "";

    @Column(name = "taxi_context", columnDefinition = "text")
    private String taxiContext;

    @Column(name = "peya_context", columnDefinition = "text")
    private String peyaContext;

    @Column(name = "serv_context", columnDefinition = "text")
    private String servContext;

    @Column(name = "temperature", nullable = false)
    private double temperature = 0.4;

    @Column(name = "max_tokens", nullable = false)
    private int maxTokens = 700;

    /** Contexto extra segun el tipo de trabajador (para inyectar en el system prompt). */
    public String contextFor(ClientType ct) {
        if (ct == null) return "";
        return switch (ct) {
            case TAXISTA -> taxiContext == null ? "" : taxiContext;
            case DELIVERY_PEYA -> peyaContext == null ? "" : peyaContext;
            case SERVICIOS_PROFESIONALES -> servContext == null ? "" : servContext;
        };
    }
}
