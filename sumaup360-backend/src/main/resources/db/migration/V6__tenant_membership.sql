-- V6: membresia usuario <-> tenant (schema tenant).
-- Vincula un usuario (auth.app_user) con un tenant. is_default marca el tenant activo por
-- defecto del usuario. Con esto el backend resuelve el tenant del contexto en cada request.

CREATE TABLE tenant.membership (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES auth.app_user (id) ON DELETE CASCADE,
    tenant_id   UUID         NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    is_default  BOOLEAN      NOT NULL DEFAULT TRUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    UNIQUE (user_id, tenant_id)
);

CREATE INDEX idx_membership_user   ON tenant.membership (user_id);
CREATE INDEX idx_membership_tenant ON tenant.membership (tenant_id);
