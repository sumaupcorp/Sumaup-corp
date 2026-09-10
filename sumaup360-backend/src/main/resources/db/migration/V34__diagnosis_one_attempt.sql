-- V34: el diagnostico con IA es gratis pero de UN solo intento por cuenta.
-- diagnosis_available = true por defecto; se apaga al generar el diagnostico. Soporte lo
-- reactiva desde el backoffice (p. ej. si el usuario cambia de RUC).

ALTER TABLE app.person_profile
    ADD COLUMN IF NOT EXISTS diagnosis_available BOOLEAN NOT NULL DEFAULT true;
