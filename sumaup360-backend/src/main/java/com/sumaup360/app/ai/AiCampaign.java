package com.sumaup360.app.ai;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Campana que aumenta temporalmente las consultas de IA gratis/premium. */
@Entity
@Table(name = "ai_campaign", schema = "app")
@Getter
@Setter
public class AiCampaign extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "client_type", length = 20)
    private String clientType; // null = todos

    @Column(name = "plan_code", length = 40)
    private String planCode; // null = todos

    @Column(name = "consultas_extra", nullable = false)
    private int consultasExtra = 0;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
