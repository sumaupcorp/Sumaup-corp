package com.sumaup360.app.ai;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Registro de cada consulta IA: auditoria + conteo de uso por periodo. */
@Entity
@Table(name = "ai_usage_log", schema = "app")
@Getter
@Setter
public class AiUsageLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "pregunta", columnDefinition = "text")
    private String pregunta;

    @Column(name = "respuesta", columnDefinition = "text")
    private String respuesta;

    @Column(name = "modelo", length = 60)
    private String modelo;

    @Column(name = "tokens")
    private Integer tokens;

    @Column(name = "tipo_consulta", length = 40)
    private String tipoConsulta; // CHAT_IA | CONSULTA_SUNAT | GUIADO

    @Column(name = "consumio_credito", nullable = false)
    private boolean consumioCredito = false;

    @Column(name = "plan_activo", length = 40)
    private String planActivo;
}
