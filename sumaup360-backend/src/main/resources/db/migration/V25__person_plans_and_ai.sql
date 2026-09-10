-- V25: planes de la Linea Personas (taxista/peya, free/premium) + configuracion y uso de IA.

-- 1) Planes Personas (billing.plan, line=PERSON). Idempotente.
INSERT INTO billing.plan (id, code, name, line, price, currency, active, created_at, updated_at)
SELECT gen_random_uuid(), v.code, v.name, 'PERSON', v.price, 'PEN', TRUE, now(), now()
FROM (VALUES
    ('taxi-free',    'Taxista Free',    0.00),
    ('taxi-premium', 'Taxista Premium', 39.90),
    ('peya-free',    'Peya Free',       0.00),
    ('peya-premium', 'Peya Premium',    29.90)
) AS v(code, name, price)
WHERE NOT EXISTS (SELECT 1 FROM billing.plan p WHERE p.code = v.code);

-- 2) Configuracion de limites de IA (NO hardcodeada, editable desde backoffice). Fila unica.
CREATE TABLE app.ai_config (
    id                     UUID PRIMARY KEY,
    free_iniciales         INT NOT NULL DEFAULT 1,   -- consultas SUNAT iniciales gratis
    free_ia                INT NOT NULL DEFAULT 2,   -- consultas IA gratis (free)
    premium_consultas      INT NOT NULL DEFAULT 15,  -- consultas IA premium
    periodo                VARCHAR(10) NOT NULL DEFAULT 'MENSUAL', -- DIARIO | MENSUAL
    activo                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO app.ai_config (id) VALUES (gen_random_uuid());

-- 3) Campanas: aumentan temporalmente las consultas (por tipo de cliente/plan).
CREATE TABLE app.ai_campaign (
    id              UUID PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL,
    client_type     VARCHAR(20),          -- null = todos
    plan_code       VARCHAR(40),          -- null = todos
    consultas_extra INT NOT NULL DEFAULT 0,
    fecha_inicio    DATE NOT NULL,
    fecha_fin       DATE NOT NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4) Log de cada consulta IA (auditoria + conteo de uso por periodo).
CREATE TABLE app.ai_usage_log (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL,
    pregunta          TEXT,
    respuesta         TEXT,
    modelo            VARCHAR(60),
    tokens            INT,
    tipo_consulta     VARCHAR(40),        -- CHAT_IA | CONSULTA_SUNAT | GUIADO
    consumio_credito  BOOLEAN NOT NULL DEFAULT FALSE,
    plan_activo       VARCHAR(40),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_usage_user_created ON app.ai_usage_log (user_id, created_at);
