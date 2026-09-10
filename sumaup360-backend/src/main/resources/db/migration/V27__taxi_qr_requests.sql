-- V27: Modulo Taxista - cliente final (global), QR y solicitudes de comprobante.

-- Cliente final del QR. GLOBAL y reutilizable: un mismo cliente puede pedir a varios taxistas.
-- Se busca por documento para precargar sus datos.
CREATE TABLE app.customer (
    id           UUID PRIMARY KEY,
    doc_type     VARCHAR(10),                 -- DNI | RUC
    doc_number   VARCHAR(15),
    name         VARCHAR(200),                -- nombre o razon social
    whatsapp     VARCHAR(30),
    email        VARCHAR(160),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_customer_doc ON app.customer (doc_number) WHERE doc_number IS NOT NULL;

-- QR del taxista (token publico, no adivinable).
CREATE TABLE app.qr_code (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL,               -- taxista (app_user)
    token        VARCHAR(64) NOT NULL UNIQUE,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_qr_user ON app.qr_code (user_id);

-- Solicitud de comprobante hecha por un cliente al escanear el QR del taxista.
CREATE TABLE app.receipt_request (
    id               UUID PRIMARY KEY,
    taxista_user_id  UUID NOT NULL,
    customer_id      UUID REFERENCES app.customer (id),
    qr_token         VARCHAR(64),
    tipo             VARCHAR(10) NOT NULL,     -- BOLETA | FACTURA
    monto            NUMERIC(12,2) NOT NULL,
    monto_editado    NUMERIC(12,2),
    doc_type         VARCHAR(10),
    doc_number       VARCHAR(15),
    customer_name    VARCHAR(200),
    whatsapp         VARCHAR(30),
    email            VARCHAR(160),
    observacion      VARCHAR(500),
    estado           VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_TAXISTA',
    motivo           VARCHAR(500),             -- motivo de rechazo/edicion/observacion
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at     TIMESTAMPTZ
);
CREATE INDEX idx_request_taxista ON app.receipt_request (taxista_user_id, created_at);
CREATE INDEX idx_request_estado ON app.receipt_request (estado);

-- Adjuntos genericos (PDF/XML/CDR de comprobantes, reportes Peya, etc.).
CREATE TABLE app.attached_file (
    id           UUID PRIMARY KEY,
    entity_type  VARCHAR(40) NOT NULL,        -- RECEIPT_REQUEST | PEYA_UPLOAD
    entity_id    UUID NOT NULL,
    file_url     TEXT NOT NULL,
    file_name    VARCHAR(200),
    uploaded_by  UUID,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_attached_entity ON app.attached_file (entity_type, entity_id);
