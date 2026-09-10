package com.sumaup360.app.honorarios.domain;

import com.sumaup360.app.honorarios.enums.SuspensionStatus;
import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Solicitud de Suspension de 4ta categoria (anual) que el backoffice tramita ante SUNAT. */
@Entity
@Table(name = "suspension_request", schema = "app")
@Getter
@Setter
public class SuspensionRequest extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "anio", nullable = false)
    private int anio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private SuspensionStatus estado = SuspensionStatus.SOLICITADA;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "constancia_url", columnDefinition = "text")
    private String constanciaUrl;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
