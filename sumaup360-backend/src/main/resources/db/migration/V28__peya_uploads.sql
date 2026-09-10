-- V28: Modulo Delivery/Peya - carga mensual de PDF de ventas y su procesamiento.

CREATE TABLE app.peya_upload (
    id                     UUID PRIMARY KEY,
    user_id                UUID NOT NULL,
    ruc                    VARCHAR(11),
    periodo                VARCHAR(7) NOT NULL,        -- AAAA-MM
    pdf_url                TEXT NOT NULL,              -- PDF original de ventas (Firebase Storage)
    estado                 VARCHAR(30) NOT NULL DEFAULT 'PDF_SUBIDO',
    observacion_usuario    VARCHAR(500),
    observacion_backoffice VARCHAR(500),
    codigo_nps             VARCHAR(40),                -- codigo NPS generado por el backoffice
    completed_at           TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_peya_user ON app.peya_upload (user_id, created_at);
CREATE INDEX idx_peya_estado ON app.peya_upload (estado);

-- Los archivos resultado (reporte, declaracion) reutilizan app.attached_file con
-- entity_type = 'PEYA_UPLOAD'.
