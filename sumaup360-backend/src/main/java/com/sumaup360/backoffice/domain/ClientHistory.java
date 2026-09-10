package com.sumaup360.backoffice.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Evento del historial de un cliente (tenant): alta, cambios de plan, credenciales, etc. */
@Entity
@Table(name = "client_history", schema = "tenant")
@Getter
@Setter
public class ClientHistory extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "actor_user_id")
    private UUID actorUserId;
}
