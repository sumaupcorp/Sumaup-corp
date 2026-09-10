-- V8: permisos del ERP core. Se suman al catalogo global de permisos.
-- Los roles de tenant (p. ej. tenant-admin, cajero) los referencian por codigo.

INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('product:read',     'Ver productos'),
    ('product:manage',   'Gestionar productos'),
    ('customer:read',    'Ver clientes'),
    ('customer:manage',  'Gestionar clientes'),
    ('inventory:read',   'Ver inventario'),
    ('inventory:adjust', 'Ajustar inventario'),
    ('pos:read',         'Ver caja/POS'),
    ('pos:operate',      'Operar caja/POS (abrir/cerrar)'),
    ('sale:read',        'Ver ventas'),
    ('sale:create',      'Registrar ventas')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

-- El rol global staff 'admin' recibe tambien los nuevos permisos.
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN
    ('product:read','product:manage','customer:read','customer:manage',
     'inventory:read','inventory:adjust','pos:read','pos:operate','sale:read','sale:create')
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;
