-- V33: Modulo Servicios Profesionales (Premium).
-- 1) Solicitud de Recibo por Honorarios: el profesional envia el formulario y el backoffice
--    le genera el recibo (adjunta el PDF en app.attached_file, entity_type='HONORARIO_REQUEST').
-- 2) Solicitud de Suspension de 4ta categoria (ANUAL): elige el año, el backoffice la tramita
--    y adjunta la constancia (entity_type='SUSPENSION_REQUEST').

CREATE TABLE app.honorario_request (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    cliente_nombre      VARCHAR(200) NOT NULL,      -- empresa/razon social que paga
    cliente_doc_type    VARCHAR(10),                -- RUC | DNI
    cliente_doc_number  VARCHAR(15),
    descripcion         VARCHAR(500) NOT NULL,      -- servicio prestado
    monto               NUMERIC(12,2) NOT NULL,
    con_retencion       BOOLEAN NOT NULL DEFAULT false,
    estado              VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    recibo_url          TEXT,                       -- PDF del recibo generado
    observacion         VARCHAR(500),
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_honorario_user ON app.honorario_request (user_id, created_at);
CREATE INDEX idx_honorario_estado ON app.honorario_request (estado);

CREATE TABLE app.suspension_request (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    anio            INT NOT NULL,                   -- año del ejercicio (suspension anual)
    estado          VARCHAR(30) NOT NULL DEFAULT 'SOLICITADA',
    observacion     VARCHAR(500),
    constancia_url  TEXT,                           -- constancia de suspension tramitada
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_suspension_user ON app.suspension_request (user_id, created_at);
CREATE INDEX idx_suspension_estado ON app.suspension_request (estado);
