-- V9: proveedores (schema erp) y permisos de proveedor.

CREATE TABLE erp.supplier (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenant.tenant (id) ON DELETE CASCADE,
    name        VARCHAR(200)  NOT NULL,
    ruc         VARCHAR(11),
    phone       VARCHAR(30),
    email       VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_supplier_tenant ON erp.supplier (tenant_id);

-- Permisos de proveedor
INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), c.code, c.description, now(), now()
FROM (VALUES
    ('supplier:read',   'Ver proveedores'),
    ('supplier:manage', 'Gestionar proveedores')
) AS c(code, description)
ON CONFLICT (code) DO NOTHING;

-- El admin global recibe los permisos de proveedor.
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code IN ('supplier:read', 'supplier:manage')
WHERE ro.code = 'admin' AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;
