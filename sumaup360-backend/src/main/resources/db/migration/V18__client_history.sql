-- V18: historial por cliente (tenant). Registra eventos relevantes hechos desde el Backoffice
-- (alta, cambios de plan/licencia, actualizacion y revelado de credenciales fiscales).

CREATE TABLE tenant.client_history (
    id             UUID         PRIMARY KEY,
    tenant_id      UUID         NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    event_type     VARCHAR(40)  NOT NULL,
    description    VARCHAR(300) NOT NULL,
    actor_user_id  UUID,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_client_history_tenant ON tenant.client_history (tenant_id, created_at DESC);
