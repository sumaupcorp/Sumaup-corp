-- V3: identidad efectiva y RBAC (schema auth).
-- El backend es la autoridad de roles/permisos. Firebase solo prueba la identidad.
-- PK UUID para entidades de dominio. Timestamps con zona (timestamptz).

-- ---------------------------------------------------------------------------
-- Usuario efectivo (vinculado a Firebase por firebase_uid)
-- ---------------------------------------------------------------------------
CREATE TABLE auth.app_user (
    id            UUID         PRIMARY KEY,
    firebase_uid  VARCHAR(128) NOT NULL UNIQUE,
    email         VARCHAR(255),
    display_name  VARCHAR(160),
    phone         VARCHAR(30),
    -- Linea/identidad: PERSON (app movil), BUSINESS (SaaS/ERP), STAFF (backoffice interno)
    user_type     VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_app_user_user_type ON auth.app_user (user_type);

-- ---------------------------------------------------------------------------
-- Permisos: catalogo global de acciones (formato recurso:accion)
-- ---------------------------------------------------------------------------
CREATE TABLE auth.permission (
    id           UUID         PRIMARY KEY,
    code         VARCHAR(80)  NOT NULL UNIQUE,
    description  VARCHAR(200),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

-- ---------------------------------------------------------------------------
-- Roles: globales/staff (tenant_id NULL) o de un tenant (tenant_id con valor)
-- ---------------------------------------------------------------------------
CREATE TABLE auth.role (
    id           UUID         PRIMARY KEY,
    code         VARCHAR(80)  NOT NULL,
    name         VARCHAR(120) NOT NULL,
    tenant_id    UUID,                          -- NULL = rol global/staff
    is_staff     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

-- Unicidad de code para roles globales (tenant_id NULL) y por tenant.
CREATE UNIQUE INDEX uq_role_code_global ON auth.role (code) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX uq_role_code_tenant ON auth.role (tenant_id, code) WHERE tenant_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Rol <-> Permiso
-- ---------------------------------------------------------------------------
CREATE TABLE auth.role_permission (
    role_id        UUID NOT NULL REFERENCES auth.role (id) ON DELETE CASCADE,
    permission_id  UUID NOT NULL REFERENCES auth.permission (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ---------------------------------------------------------------------------
-- Usuario <-> Rol
-- ---------------------------------------------------------------------------
CREATE TABLE auth.user_role (
    user_id  UUID NOT NULL REFERENCES auth.app_user (id) ON DELETE CASCADE,
    role_id  UUID NOT NULL REFERENCES auth.role (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_role_role ON auth.user_role (role_id);
