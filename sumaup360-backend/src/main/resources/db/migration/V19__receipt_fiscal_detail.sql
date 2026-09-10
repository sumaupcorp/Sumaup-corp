-- V19: detalle fiscal de recibos para el Backoffice (Linea Personas).
-- - Clave SOL del usuario (persona) cifrada, en su perfil.
-- - Estados de declaracion en SIRE y en SUNAT por recibo, con periodo y fecha.

ALTER TABLE app.person_profile
    ADD COLUMN sol_user     VARCHAR(64),
    ADD COLUMN sol_pass_enc TEXT;

ALTER TABLE app.receipt
    ADD COLUMN issuer_ruc        VARCHAR(11),
    ADD COLUMN declared_sire     BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN sire_period       VARCHAR(7),
    ADD COLUMN sire_declared_at  TIMESTAMPTZ,
    ADD COLUMN declared_sunat    BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN sunat_period      VARCHAR(7),
    ADD COLUMN sunat_declared_at TIMESTAMPTZ;

-- Permiso para declarar en SIRE/SUNAT (contabilidad)
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), 'receipt:declare', 'Declarar recibos en SIRE/SUNAT', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM auth.permission WHERE code = 'receipt:declare');

INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code = 'receipt:declare'
WHERE ro.code IN ('admin', 'contador') AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;
