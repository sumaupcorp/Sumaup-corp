package com.sumaup360.app.taxi.domain;

import com.sumaup360.app.taxi.enums.ComprobanteType;
import com.sumaup360.app.taxi.enums.RequestStatus;
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

/** Solicitud de comprobante hecha por un cliente al escanear el QR del taxista. */
@Entity
@Table(name = "receipt_request", schema = "app")
@Getter
@Setter
public class ReceiptRequest extends BaseEntity {

    @Column(name = "taxista_user_id", nullable = false)
    private UUID taxistaUserId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "qr_token", length = 64)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private ComprobanteType tipo;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "monto_editado", precision = 12, scale = 2)
    private BigDecimal montoEditado;

    @Column(name = "doc_type", length = 10)
    private String docType;

    @Column(name = "doc_number", length = 15)
    private String docNumber;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "whatsapp", length = 30)
    private String whatsapp;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private RequestStatus estado = RequestStatus.PENDIENTE_TAXISTA;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
