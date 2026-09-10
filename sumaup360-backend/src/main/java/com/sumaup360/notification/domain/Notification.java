package com.sumaup360.notification.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Notificacion in-app dirigida a un usuario. */
@Entity
@Table(name = "notification", schema = "notification")
@Getter
@Setter
public class Notification extends BaseEntity {

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "body", length = 1000)
    private String body;

    @Column(name = "type", nullable = false, length = 30)
    private String type = "INFO";

    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}
