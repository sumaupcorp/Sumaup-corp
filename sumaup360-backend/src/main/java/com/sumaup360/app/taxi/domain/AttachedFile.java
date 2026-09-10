package com.sumaup360.app.taxi.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Adjunto generico (PDF/XML/CDR de comprobante, reporte Peya, etc.). */
@Entity
@Table(name = "attached_file", schema = "app")
@Getter
@Setter
public class AttachedFile extends BaseEntity {

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType; // RECEIPT_REQUEST | PEYA_UPLOAD

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "file_url", columnDefinition = "text", nullable = false)
    private String fileUrl;

    @Column(name = "file_name", length = 200)
    private String fileName;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;
}
