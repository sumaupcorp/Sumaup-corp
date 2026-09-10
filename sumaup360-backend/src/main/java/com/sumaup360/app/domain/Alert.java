package com.sumaup360.app.domain;

import com.sumaup360.app.enums.AlertStatus;
import com.sumaup360.app.enums.AlertType;
import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** Alerta/recordatorio para la persona. */
@Entity
@Table(name = "alert", schema = "app")
@Getter
@Setter
public class Alert extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AlertType type = AlertType.INFO;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status = AlertStatus.PENDING;
}
