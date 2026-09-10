-- V14: Billing (planes, suscripciones) + soporte de Backoffice (intake de recibos).
-- El plan define el TECHO de modulos (allowedModules) que se aplica sobre enabledModules.
-- Suscripcion: TENANT (licencia del SaaS) o USER (plan de la persona).

-- ---------------------------------------------------------------------------
-- Planes
-- ---------------------------------------------------------------------------
CREATE TABLE billing.plan (
    id            UUID          PRIMARY KEY,
    code          VARCHAR(40)   NOT NULL UNIQUE,
    name          VARCHAR(120)  NOT NULL,
    line          VARCHAR(20)   NOT NULL,           -- PERSON | BUSINESS
    price         NUMERIC(10,2) NOT NULL DEFAULT 0,
    currency      VARCHAR(3)    NOT NULL DEFAULT 'PEN',
    max_branches  INT,
    max_users     INT,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ   NOT NULL
);

-- Modulos permitidos por plan (techo). Solo aplica a planes BUSINESS.
CREATE TABLE billing.plan_module (
    plan_id      UUID        NOT NULL REFERENCES billing.plan (id) ON DELETE CASCADE,
    module_code  VARCHAR(40) NOT NULL,
    PRIMARY KEY (plan_id, module_code)
);

-- Suscripciones / membresias
CREATE TABLE billing.subscription (
    id               UUID         PRIMARY KEY,
    plan_id          UUID         NOT NULL REFERENCES billing.plan (id),
    subscriber_type  VARCHAR(20)  NOT NULL,         -- TENANT | USER
    tenant_id        UUID,
    user_id          UUID,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- TRIAL, ACTIVE, SUSPENDED, CANCELLED
    start_date       DATE,
    end_date         DATE,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_subscription_tenant ON billing.subscription (tenant_id);
CREATE INDEX idx_subscription_user   ON billing.subscription (user_id);

-- ---------------------------------------------------------------------------
-- Seeds de planes
-- ---------------------------------------------------------------------------
INSERT INTO billing.plan (id, code, name, line, price, max_branches, max_users, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.name, c.line, c.price, c.mb, c.mu, now(), now()
FROM (VALUES
    ('basico',     'Plan Basico',     'PERSON',   0.00, NULL, NULL),
    ('emprende',   'Plan Emprende',   'PERSON',  19.90, NULL, NULL),
    ('pro',        'Plan Pro',        'PERSON',  39.90, NULL, NULL),
    ('erp-basico', 'ERP Basico',      'BUSINESS', 49.90, 1, 3),
    ('erp-full',   'ERP Full',        'BUSINESS', 99.90, 10, 25)
) AS c(code, name, line, price, mb, mu)
ON CONFLICT (code) DO NOTHING;

-- erp-basico: solo modulos core.
INSERT INTO billing.plan_module (plan_id, module_code)
SELECT p.id, m.code
FROM billing.plan p
JOIN catalog.modules m ON m.is_core = TRUE
WHERE p.code = 'erp-basico'
ON CONFLICT DO NOTHING;

-- erp-full: todos los modulos activos.
INSERT INTO billing.plan_module (plan_id, module_code)
SELECT p.id, m.code
FROM billing.plan p
JOIN catalog.modules m ON m.is_active = TRUE
WHERE p.code = 'erp-full'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Permisos de planes (license:read/manage y receipt:read/process ya existen en V5/V11... )
-- ---------------------------------------------------------------------------
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('plan:read',   'Ver planes'),
    ('plan:manage', 'Gestionar planes')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN ('plan:read', 'plan:manage')
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Soporte de intake de recibos en el Backoffice (sobre app.receipt)
-- ---------------------------------------------------------------------------
ALTER TABLE app.receipt ADD COLUMN IF NOT EXISTS assigned_staff_id UUID;
ALTER TABLE app.receipt ADD COLUMN IF NOT EXISTS processed_at TIMESTAMPTZ;
