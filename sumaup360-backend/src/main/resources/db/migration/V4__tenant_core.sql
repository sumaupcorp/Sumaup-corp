-- V4: nucleo multi-tenant (schema tenant).
-- Jerarquia: tenant -> company (empresa) -> branch (sucursal).
-- Todo recurso de Negocios se aisla por tenant_id. PK UUID.

-- ---------------------------------------------------------------------------
-- Tenant: la cuenta cliente (Linea Negocios). Un tenant agrupa empresas.
-- ---------------------------------------------------------------------------
CREATE TABLE tenant.tenant (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL UNIQUE,   -- slug legible
    name        VARCHAR(160) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

-- ---------------------------------------------------------------------------
-- Company (empresa) dentro de un tenant.
-- business_type_code / vertical_code referencian catalogos (catalog.*) por codigo.
-- ---------------------------------------------------------------------------
CREATE TABLE tenant.company (
    id                  UUID         PRIMARY KEY,
    tenant_id           UUID         NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    legal_name          VARCHAR(200) NOT NULL,
    ruc                 VARCHAR(11),
    business_type_code  VARCHAR(40),
    vertical_code       VARCHAR(40),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_company_tenant ON tenant.company (tenant_id);

-- ---------------------------------------------------------------------------
-- Branch (sucursal) de una empresa.
-- ---------------------------------------------------------------------------
CREATE TABLE tenant.branch (
    id          UUID         PRIMARY KEY,
    tenant_id   UUID         NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    company_id  UUID         NOT NULL REFERENCES tenant.company (id) ON DELETE CASCADE,
    name        VARCHAR(160) NOT NULL,
    address     VARCHAR(255),
    is_main     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_branch_tenant ON tenant.branch (tenant_id);
CREATE INDEX idx_branch_company ON tenant.branch (company_id);
