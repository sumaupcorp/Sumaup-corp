-- V31: campos del nuevo onboarding (wizard de 3 pasos) para la Linea Personas.
-- Nombre y apellido separados; onboarding_completed separa "termino el onboarding" (entra al
-- Home) de profile_completed (que exige RUC validado ACTIVO+HABIDO en SUNAT).

ALTER TABLE app.person_profile
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(80),
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(80),
    ADD COLUMN IF NOT EXISTS onboarding_completed BOOLEAN NOT NULL DEFAULT false;
