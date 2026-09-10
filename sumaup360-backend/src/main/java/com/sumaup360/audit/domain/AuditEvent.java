package com.sumaup360.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Evento de auditoria (append-only). Lo escribe el AuditInterceptor en cada mutacion. */
@Entity
@Table(name = "audit_event", schema = "audit")
@Getter
@Setter
public class AuditEvent {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_type", length = 20)
    private String actorType;

    @Column(name = "method", length = 10)
    private String method;

    @Column(name = "path", length = 300)
    private String path;

    @Column(name = "action", length = 160)
    private String action;

    @Column(name = "status")
    private Integer status;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "ip", length = 60)
    private String ip;
}
