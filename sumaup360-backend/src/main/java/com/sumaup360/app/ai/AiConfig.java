package com.sumaup360.app.ai;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Configuracion de limites de IA (editable desde backoffice). Fila unica. */
@Entity
@Table(name = "ai_config", schema = "app")
@Getter
@Setter
public class AiConfig extends BaseEntity {

    @Column(name = "free_iniciales", nullable = false)
    private int freeIniciales = 1;

    @Column(name = "free_ia", nullable = false)
    private int freeIa = 2;

    @Column(name = "premium_consultas", nullable = false)
    private int premiumConsultas = 15;

    /** DIARIO | MENSUAL */
    @Column(name = "periodo", nullable = false, length = 10)
    private String periodo = "MENSUAL";

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
