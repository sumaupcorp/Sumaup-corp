-- V22: datos fiscales de la Ficha RUC (SUNAT) en el perfil de la persona.
-- Los consulta el microservicio sumaup360-sunat y alimentan el diagnostico con IA.

ALTER TABLE app.person_profile
    ADD COLUMN tax_status        VARCHAR(40),    -- Estado del contribuyente (ACTIVO, ...)
    ADD COLUMN tax_condition     VARCHAR(40),    -- Condicion (HABIDO, NO HABIDO, ...)
    ADD COLUMN taxpayer_type     VARCHAR(80),    -- Tipo contribuyente (PERSONA NATURAL SIN NEGOCIO, ...)
    ADD COLUMN economic_activity VARCHAR(200),   -- Actividad economica principal (descripcion)
    ADD COLUMN ciiu_code         VARCHAR(10),    -- Codigo CIIU de la actividad principal
    ADD COLUMN ruc_checked_at    TIMESTAMPTZ;    -- Ultima consulta exitosa a SUNAT
