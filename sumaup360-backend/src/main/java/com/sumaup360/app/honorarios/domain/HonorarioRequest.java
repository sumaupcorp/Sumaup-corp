package com.sumaup360.app.honorarios.domain;

import com.sumaup360.app.honorarios.enums.HonorarioStatus;
import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Solicitud de Recibo por Honorarios que envia un profesional para que el backoffice lo genere. */
@Entity
@Table(name = "honorario_request", schema = "app")
@Getter
@Setter
public class HonorarioRequest extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "cliente_nombre", nullable = false, length = 200)
    private String clienteNombre;

    @Column(name = "cliente_doc_type", length = 10)
    private String clienteDocType;

    @Column(name = "cliente_doc_number", length = 15)
    private String clienteDocNumber;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "con_retencion", nullable = false)
    private boolean conRetencion = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private HonorarioStatus estado = HonorarioStatus.PENDIENTE;

    @Column(name = "recibo_url", columnDefinition = "text")
    private String reciboUrl;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
