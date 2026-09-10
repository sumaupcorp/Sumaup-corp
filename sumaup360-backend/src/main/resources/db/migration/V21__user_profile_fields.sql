-- V21: campos del perfil de usuario (Linea Personas) para completar perfil desde la app movil.

ALTER TABLE auth.app_user
    ADD COLUMN photo_url VARCHAR(500);

ALTER TABLE app.person_profile
    ADD COLUMN country_code      VARCHAR(5)   DEFAULT '+51',
    ADD COLUMN referral_code     VARCHAR(40),
    ADD COLUMN profile_completed BOOLEAN      NOT NULL DEFAULT FALSE;
