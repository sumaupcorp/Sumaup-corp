package com.sumaup360.app.peya.domain;

import com.sumaup360.app.peya.enums.PeyaStatus;
import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Carga mensual de PDF de ventas Peya y su procesamiento. */
@Entity
@Table(name = "peya_upload", schema = "app")
@Getter
@Setter
public class PeyaUpload extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @Column(name = "periodo", nullable = false, length = 7)
    private String periodo; // AAAA-MM

    @Column(name = "pdf_url", columnDefinition = "text", nullable = false)
    private String pdfUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private PeyaStatus estado = PeyaStatus.PDF_SUBIDO;

    @Column(name = "observacion_usuario", length = 500)
    private String observacionUsuario;

    @Column(name = "observacion_backoffice", length = 500)
    private String observacionBackoffice;

    @Column(name = "codigo_nps", length = 40)
    private String codigoNps;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
