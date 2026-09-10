-- V5: semillas de RBAC. Permisos globales y roles staff (Backoffice interno).
-- Los roles de tenant (cajero, almacen, etc.) se crean por tenant en fases siguientes.
-- gen_random_uuid() disponible en PostgreSQL core. now() para timestamps.

-- ---------------------------------------------------------------------------
-- Permisos (formato recurso:accion)
-- ---------------------------------------------------------------------------
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('staff:read',     'Ver personal interno (staff)'),
    ('staff:manage',   'Gestionar personal interno (staff)'),
    ('role:read',      'Ver roles y permisos'),
    ('role:manage',    'Gestionar roles y permisos'),
    ('tenant:read',    'Ver tenants/empresas'),
    ('tenant:manage',  'Gestionar tenants/empresas'),
    ('license:read',   'Ver licencias/membresias'),
    ('license:manage', 'Gestionar licencias/membresias'),
    ('receipt:read',   'Ver comprobantes recibidos de la app'),
    ('receipt:process','Procesar comprobantes (cargar en SUMAUP360)'),
    ('support:read',   'Ver soporte/tickets de usuarios de la app'),
    ('support:manage', 'Atender soporte/tickets'),
    ('report:read',    'Ver reportes y KPIs'),
    ('audit:read',     'Ver bitacora de auditoria'),
    ('company:read',   'Ver empresas (negocio)'),
    ('company:manage', 'Gestionar empresas (negocio)'),
    ('branch:read',    'Ver sucursales'),
    ('branch:manage',  'Gestionar sucursales'),
    ('user:read',      'Ver usuarios del tenant'),
    ('user:manage',    'Gestionar usuarios del tenant')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Roles staff (globales: tenant_id NULL, is_staff TRUE)
-- ---------------------------------------------------------------------------
INSERT INTO auth.role (id, code, name, tenant_id, is_staff, created_at, updated_at)
SELECT gen_random_uuid(), r.code, r.name, NULL, TRUE, now(), now()
FROM (VALUES
    ('admin',        'Administrador / Super-admin'),
    ('gerencia',     'Gerencia'),
    ('contador',     'Contador'),
    ('desarrollador','Desarrollador'),
    ('soporte',      'Soporte'),
    ('logistica',    'Logistica')
) AS r(code, name)
WHERE NOT EXISTS (
    SELECT 1 FROM auth.role e WHERE e.code = r.code AND e.tenant_id IS NULL
);

-- ---------------------------------------------------------------------------
-- Asignacion de permisos a roles staff
-- ---------------------------------------------------------------------------

-- admin: TODOS los permisos
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
CROSS JOIN auth.permission pe
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- Helper conceptual: el resto se asigna por lista explicita de codigos.
-- gerencia: lectura transversal + reportes
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN
    ('staff:read','role:read','tenant:read','license:read','receipt:read','support:read','report:read','audit:read')
WHERE ro.code = 'gerencia' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- contador: nucleo de comprobantes + soporte
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN
    ('receipt:read','receipt:process','support:read','support:manage','tenant:read')
WHERE ro.code = 'contador' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- desarrollador: tecnico / lectura
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN
    ('audit:read','report:read','tenant:read','staff:read','role:read')
WHERE ro.code = 'desarrollador' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- soporte: tickets y usuarios de la app
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN
    ('support:read','support:manage','receipt:read','tenant:read')
WHERE ro.code = 'soporte' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;

-- logistica: operacion / lectura
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN
    ('tenant:read','report:read')
WHERE ro.code = 'logistica' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;
