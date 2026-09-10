-- V13: Linea Personas (schema app). Datos de la app movil, scopeados por USUARIO (no tenant).
-- Cada persona ve solo lo suyo (user_id = auth.app_user.id). Diagnostico, ingresos, gastos,
-- recibos y alertas. El procesamiento de recibos lo hara el Backoffice (Fase 8).

-- Perfil tributario de la persona
CREATE TABLE app.person_profile (
    id            UUID         PRIMARY KEY,
    user_id       UUID         NOT NULL UNIQUE REFERENCES auth.app_user (id) ON DELETE CASCADE,
    segment_code  VARCHAR(40),               -- catalog.person_segments
    ruc           VARCHAR(11),
    regime        VARCHAR(40),               -- regimen tributario (texto, sin logica definitiva)
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

-- Diagnostico tributario (se guarda cada vez; el ultimo es el vigente)
CREATE TABLE app.tax_diagnosis (
    id                     UUID          PRIMARY KEY,
    user_id                UUID          NOT NULL REFERENCES auth.app_user (id) ON DELETE CASCADE,
    segment_code           VARCHAR(40),
    monthly_income         NUMERIC(12,2),
    recommended_plan_code  VARCHAR(40),
    answers                JSONB,
    created_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_diagnosis_user ON app.tax_diagnosis (user_id);

-- Ingresos
CREATE TABLE app.income (
    id              UUID          PRIMARY KEY,
    user_id         UUID          NOT NULL REFERENCES auth.app_user (id) ON DELETE CASCADE,
    tx_date         DATE          NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'PEN',
    category        VARCHAR(60),
    payment_method  VARCHAR(30),
    description     VARCHAR(255),
    receipt_id      UUID,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_income_user ON app.income (user_id, tx_date);

-- Gastos
CREATE TABLE app.expense (
    id              UUID          PRIMARY KEY,
    user_id         UUID          NOT NULL REFERENCES auth.app_user (id) ON DELETE CASCADE,
    tx_date         DATE          NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'PEN',
    category        VARCHAR(60),
    payment_method  VARCHAR(30),
    description     VARCHAR(255),
    receipt_id      UUID,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_expense_user ON app.expense (user_id, tx_date);

-- Recibos/comprobantes subidos por la persona
CREATE TABLE app.receipt (
    id          UUID          PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES auth.app_user (id) ON DELETE CASCADE,
    type        VARCHAR(20)   NOT NULL DEFAULT 'OTHER',   -- BOLETA, FACTURA, RHE, OTHER
    doc_number  VARCHAR(40),
    issue_date  DATE,
    amount      NUMERIC(12,2),
    currency    VARCHAR(3)    NOT NULL DEFAULT 'PEN',
    file_url    VARCHAR(500),
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING, IN_PROCESS, PROCESSED, OBSERVED
    notes       VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_receipt_user ON app.receipt (user_id);

-- Alertas tributarias y recordatorios
CREATE TABLE app.alert (
    id          UUID          PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES auth.app_user (id) ON DELETE CASCADE,
    type        VARCHAR(20)   NOT NULL DEFAULT 'INFO',    -- TAX_DEADLINE, INFO, WARNING, URGENT
    title       VARCHAR(160)  NOT NULL,
    message     VARCHAR(500),
    due_date    DATE,
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING, READ, RESOLVED
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_alert_user ON app.alert (user_id, status);
