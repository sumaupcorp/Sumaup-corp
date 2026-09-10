package com.sumaup360.notification.domain;

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

/**
 * Campana de push (publicidad/recordatorio) creada desde el Backoffice. Se envia de
 * inmediato o en la fecha programada a la audiencia elegida (usuarios de la app movil).
 */
@Entity
@Table(name = "campaign", schema = "notification")
@Getter
@Setter
public class Campaign extends BaseEntity {

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    /** Ruta de la app a abrir al tocar el push (opcional), p.ej. /home. */
    @Column(name = "route", length = 120)
    private String route;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience", nullable = false, length = 30)
    private CampaignAudience audience;

    /** NULL = envio inmediato al crearla. */
    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private CampaignStatus status = CampaignStatus.SCHEDULED;

    /** Usuarios a los que se intento enviar (tenian dispositivos registrados). */
    @Column(name = "sent_count", nullable = false)
    private int sentCount;

    /** Usuarios de la audiencia sin dispositivos registrados (no se pudo enviar). */
    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    /** Staff que creo la campana. */
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;
}
