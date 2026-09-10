-- V26: permisos de backoffice para administrar planes premium y la IA (Linea Personas).

INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('person:plan:manage', 'Activar/cancelar el plan premium de una persona'),
    ('ai:manage',          'Editar limites y campanas de IA'),
    ('ai:usage:read',      'Ver el consumo de IA de las personas')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

-- admin (superadmin staff) recibe todo
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN ('person:plan:manage','ai:manage','ai:usage:read')
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- gerencia: solo lectura del consumo de IA
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN ('ai:usage:read')
WHERE ro.code = 'gerencia' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;
