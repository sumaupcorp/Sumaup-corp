-- V29: Diagnostico con IA (Gemini). Guarda el contexto usado y el resultado, con historico.

CREATE TABLE app.diagnosis_report (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL,
    contexto     TEXT,                       -- datos del usuario usados en el prompt
    resultado    TEXT NOT NULL,              -- diagnostico generado
    modelo       VARCHAR(60),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_diagnosis_report_user ON app.diagnosis_report (user_id, created_at);
