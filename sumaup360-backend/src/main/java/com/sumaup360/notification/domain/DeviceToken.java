package com.sumaup360.notification.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Token FCM de un dispositivo de la app movil (Linea Personas). El token es unico:
 * si otro usuario inicia sesion en el mismo dispositivo, el token se reasigna.
 */
@Entity
@Table(name = "device_token", schema = "notification")
@Getter
@Setter
public class DeviceToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false, unique = true, columnDefinition = "text")
    private String token;

    /** android | ios | web */
    @Column(name = "platform", nullable = false, length = 10)
    private String platform;
}
