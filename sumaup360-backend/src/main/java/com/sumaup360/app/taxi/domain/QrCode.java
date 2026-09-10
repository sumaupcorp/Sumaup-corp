package com.sumaup360.app.taxi.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** QR publico del taxista (token no adivinable). */
@Entity
@Table(name = "qr_code", schema = "app")
@Getter
@Setter
public class QrCode extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId; // taxista

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
