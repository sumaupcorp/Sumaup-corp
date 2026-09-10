-- V17: bóveda de credenciales fiscales (Clave SOL / RUC) gestionada por el Backoffice.
-- La clave SOL se guarda SIEMPRE cifrada (AES-GCM). Solo se descifra al autorizar (auditado);
-- nunca se persiste en texto plano. Permisos de backoffice para clientes y bóveda.

CREATE TABLE tenant.fiscal_credentials (
    id            UUID         PRIMARY KEY,
    tenant_id     UUID         NOT NULL UNIQUE REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    ruc           VARCHAR(11),
    sol_user      VARCHAR(64),
    sol_pass_enc  TEXT,                       -- contrasena SOL cifrada (AES-GCM, base64)
    updated_by    UUID,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

-- Permisos del backoffice (cliente y boveda de credenciales)
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('client:read',  'Ver clientes (tenants) del SaaS'),
    ('sol:read',     'Ver credenciales fiscales (enmascaradas)'),
    ('sol:manage',   'Registrar/actualizar credenciales fiscales'),
    ('sol:reveal',   'Revelar (descifrar) la Clave SOL, auditado')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

-- admin (superadmin staff) recibe todo
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN ('client:read','sol:read','sol:manage','sol:reveal')
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- contador: ver clientes, ver y revelar credenciales (para sanear contabilidad)
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN ('client:read','sol:read','sol:reveal')
WHERE ro.code = 'contador' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- gerencia: solo ver clientes y credenciales enmascaradas
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN ('client:read','sol:read')
WHERE ro.code = 'gerencia' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;
