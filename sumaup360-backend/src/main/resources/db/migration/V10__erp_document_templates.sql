-- V10: modulo de plantillas de documentos comerciales (schema erp).
-- Multi-tenant: todo lleva tenant_id (y company_id). Una plantilla base configurable por
-- formato; los campos por rubro son opcionales (no se duplica por rubro).
-- Preparado para facturacion electronica futura (series, correlativos, xml/cdr/qr/hash).

-- ---------------------------------------------------------------------------
-- Plantillas (config visual y de campos)
-- ---------------------------------------------------------------------------
CREATE TABLE erp.document_templates (
    id                          UUID          PRIMARY KEY,
    tenant_id                   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    company_id                  UUID          NOT NULL REFERENCES tenant.company (id) ON DELETE CASCADE,
    branch_id                   UUID          REFERENCES tenant.branch (id),
    business_type_id            UUID,                          -- rubro opcional (catalogo)
    document_type               VARCHAR(40)   NOT NULL,
    print_format                VARCHAR(20)   NOT NULL,
    template_name               VARCHAR(160)  NOT NULL,
    template_code               VARCHAR(60)   NOT NULL,
    is_default                  BOOLEAN       NOT NULL DEFAULT FALSE,
    active                      BOOLEAN       NOT NULL DEFAULT TRUE,
    primary_color               VARCHAR(20)   NOT NULL DEFAULT '#0B5BFF',
    secondary_color             VARCHAR(20)   NOT NULL DEFAULT '#102A4C',
    font_family                 VARCHAR(80)   NOT NULL DEFAULT 'Helvetica, Arial, sans-serif',
    logo_url                    VARCHAR(500),
    show_logo                   BOOLEAN       NOT NULL DEFAULT TRUE,
    show_qr                     BOOLEAN       NOT NULL DEFAULT FALSE,
    show_payment_info           BOOLEAN       NOT NULL DEFAULT TRUE,
    show_seller                 BOOLEAN       NOT NULL DEFAULT TRUE,
    show_customer_address       BOOLEAN       NOT NULL DEFAULT TRUE,
    show_business_extra_fields  BOOLEAN       NOT NULL DEFAULT FALSE,
    header_config               JSONB,
    body_config                 JSONB,
    footer_config               JSONB,
    custom_css                  TEXT,
    created_at                  TIMESTAMPTZ   NOT NULL,
    updated_at                  TIMESTAMPTZ   NOT NULL,
    UNIQUE (tenant_id, company_id, template_code)
);
CREATE INDEX idx_doc_tpl_tenant_company ON erp.document_templates (tenant_id, company_id);
CREATE INDEX idx_doc_tpl_type ON erp.document_templates (company_id, document_type);

-- ---------------------------------------------------------------------------
-- Series y correlativos (preparado para SUNAT)
-- ---------------------------------------------------------------------------
CREATE TABLE erp.document_series (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    company_id      UUID          NOT NULL REFERENCES tenant.company (id) ON DELETE CASCADE,
    branch_id       UUID          REFERENCES tenant.branch (id),
    document_type   VARCHAR(40)   NOT NULL,
    series          VARCHAR(10)   NOT NULL,
    current_number  BIGINT        NOT NULL DEFAULT 0,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    UNIQUE (tenant_id, company_id, document_type, series)
);
CREATE INDEX idx_doc_series_company ON erp.document_series (tenant_id, company_id);

-- ---------------------------------------------------------------------------
-- Documentos emitidos (metadatos). Preparado para FE: xml/cdr/qr/hash/sunat_status.
-- ---------------------------------------------------------------------------
CREATE TABLE erp.issued_documents (
    id                  UUID          PRIMARY KEY,
    tenant_id           UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    company_id          UUID          NOT NULL REFERENCES tenant.company (id) ON DELETE CASCADE,
    branch_id           UUID          REFERENCES tenant.branch (id),
    document_type       VARCHAR(40)   NOT NULL,
    document_status     VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    series              VARCHAR(10),
    number              BIGINT,
    full_number         VARCHAR(40),
    customer_id         UUID,
    sale_id             UUID,
    quotation_id        UUID,
    delivery_guide_id   UUID,
    subtotal            NUMERIC(12,2) NOT NULL DEFAULT 0,
    igv                 NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_total      NUMERIC(12,2) NOT NULL DEFAULT 0,
    total               NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'PEN',
    pdf_url             VARCHAR(500),
    xml_url             VARCHAR(500),   -- TODO SUNAT: ruta del XML firmado
    cdr_url             VARCHAR(500),   -- TODO SUNAT: ruta del CDR de respuesta
    qr_value            VARCHAR(500),   -- TODO SUNAT: contenido del QR
    hash_value          VARCHAR(200),   -- TODO SUNAT: hash del comprobante
    sunat_status        VARCHAR(40),    -- TODO SUNAT: estado ante SUNAT
    issued_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_issued_doc_company ON erp.issued_documents (tenant_id, company_id);
CREATE INDEX idx_issued_doc_type ON erp.issued_documents (company_id, document_type);

-- ---------------------------------------------------------------------------
-- Auditoria de cambios de plantilla
-- ---------------------------------------------------------------------------
CREATE TABLE erp.document_template_audit (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL,
    company_id      UUID          NOT NULL,
    template_id     UUID          NOT NULL,
    action          VARCHAR(40)   NOT NULL,   -- CREATE, UPDATE, SET_DEFAULT
    previous_data   JSONB,
    new_data        JSONB,
    user_id         UUID,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_doc_tpl_audit_template ON erp.document_template_audit (template_id);

-- ---------------------------------------------------------------------------
-- Permisos del modulo
-- ---------------------------------------------------------------------------
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('document-template:read',   'Ver plantillas de documentos'),
    ('document-template:manage', 'Gestionar plantillas de documentos'),
    ('document-series:read',     'Ver series de documentos'),
    ('document-series:manage',   'Gestionar series de documentos'),
    ('document:render',          'Generar/previsualizar documentos (PDF)')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN
    ('document-template:read','document-template:manage',
     'document-series:read','document-series:manage','document:render')
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;
