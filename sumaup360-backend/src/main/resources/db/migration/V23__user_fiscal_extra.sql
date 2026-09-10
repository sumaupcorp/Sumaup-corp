-- V23: datos extra de la Ficha RUC (SUNAT) + volcado literal completo para analisis/IA.

ALTER TABLE app.person_profile
    ADD COLUMN razon_social             VARCHAR(200),  -- nombre / razon social del contribuyente
    ADD COLUMN fecha_inscripcion        VARCHAR(20),   -- fecha de inscripcion en SUNAT
    ADD COLUMN fecha_inicio_actividades VARCHAR(20),   -- fecha de inicio de actividades
    ADD COLUMN domicilio_fiscal         VARCHAR(300),  -- domicilio fiscal
    ADD COLUMN sunat_raw                TEXT;          -- ficha completa (JSON literal) para analisis futuro
