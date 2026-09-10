package com.sumaup360.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Configuracion de una notificacion automatica: eventos (RECEIPT_PROCESSED,
 * RECEIPT_OBSERVED) y recordatorios diarios (REMINDER_*). El staff puede activarla,
 * desactivarla y editar sus textos desde el Backoffice. Se siembra en la migracion V58.
 */
@Entity
@Table(name = "notification_setting", schema = "notification")
@Getter
@Setter
public class NotificationSetting {

    @Id
    @Column(name = "key", nullable = false, length = 40)
    private String key;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    /** Explica al staff cuando se envia esta notificacion. */
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onWrite() {
        this.updatedAt = OffsetDateTime.now();
    }
}
