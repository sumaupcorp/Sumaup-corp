package com.sumaup360.app.aidiagnosis;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Reporte de diagnostico generado con IA (Gemini). Con historico por usuario. */
@Entity
@Table(name = "diagnosis_report", schema = "app")
@Getter
@Setter
public class DiagnosisReport extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "contexto", columnDefinition = "text")
    private String contexto;

    @Column(name = "resultado", columnDefinition = "text", nullable = false)
    private String resultado;

    @Column(name = "modelo", length = 60)
    private String modelo;
}
