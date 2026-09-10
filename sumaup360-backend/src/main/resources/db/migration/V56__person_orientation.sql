-- V56: orientacion tributaria del onboarding (Linea Personas, usuarios SIN RUC).
-- El usuario responde 5 preguntas segun su actividad y la IA (o un fallback de reglas)
-- le recomienda como inscribirse ante SUNAT y en que regimen. orientation_status:
-- NULL | 'PENDING' | 'COMPLETED' | 'NOT_REQUIRED'. orientation_result guarda el JSON
-- del resultado (headline, regime, summary, reasons, modelo).

ALTER TABLE app.person_profile ADD COLUMN IF NOT EXISTS orientation_status VARCHAR(20);
ALTER TABLE app.person_profile ADD COLUMN IF NOT EXISTS orientation_result TEXT;
