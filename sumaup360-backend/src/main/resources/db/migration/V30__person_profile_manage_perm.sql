-- V30: permiso de backoffice para cambiar el tipo de cliente/rubro (TAXISTA <-> DELIVERY_PEYA)
-- de un usuario de la app. El usuario elige su tipo UNA vez al crear la cuenta y ya no lo
-- puede cambiar desde el perfil; solo el staff (soporte/admin) puede reasignarlo.

INSERT INTO auth.permission (id, code, description, created_at, updated_at)
SELECT gen_random_uuid(), 'person:profile:manage',
       'Cambiar el tipo de cliente/rubro de una persona', now(), now()
ON CONFLICT (code) DO NOTHING;

-- admin (superadmin staff) y soporte pueden reasignar el rubro
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT ro.id, pe.id
FROM auth.role ro
JOIN auth.permission pe ON pe.code = 'person:profile:manage'
WHERE ro.code IN ('admin', 'soporte') AND ro.tenant_id IS NULL
ON CONFLICT DO NOTHING;
