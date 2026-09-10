-- V40: reserva de citas online (QR publico + formulario dinamico configurable).
-- La empresa configura su pagina de reserva (logo, textos, campos) y comparte un QR que
-- apunta a la web publica (reborn) /reservas/{token}. La reserva crea una cita REQUESTED.

CREATE TABLE erp.booking_page (
    id           UUID          PRIMARY KEY,
    tenant_id    UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    company_id   UUID          NOT NULL REFERENCES tenant.company (id) ON DELETE CASCADE,
    token        VARCHAR(60)   NOT NULL UNIQUE,      -- token publico del QR
    enabled      BOOLEAN       NOT NULL DEFAULT FALSE,
    title        VARCHAR(120),                       -- nombre visible (default: nombre de la empresa)
    logo_url     VARCHAR(500),                       -- imagen/logo subido por el negocio
    welcome_text VARCHAR(300),
    form_config  TEXT          NOT NULL DEFAULT '[]', -- JSON: campos del formulario (key,label,type,required)
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,
    UNIQUE (tenant_id, company_id)
);
CREATE INDEX idx_booking_page_tenant ON erp.booking_page (tenant_id);

-- La cita guarda el origen y las respuestas del formulario dinamico.
ALTER TABLE erp.appointment
    ADD COLUMN form_data TEXT,
    ADD COLUMN source    VARCHAR(20) NOT NULL DEFAULT 'INTERNAL';   -- INTERNAL | ONLINE
