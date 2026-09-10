-- V16: plan FREE para negocios (Linea Negocios). Todo cliente nuevo lo recibe al registrarse.
-- Permite todos los modulos activos (sin techo por modulo); la monetizacion futura sera por
-- limites (sucursales/usuarios) o upgrade a planes pagos.

INSERT INTO billing.plan (id, code, name, line, price, max_branches, max_users, created_at, updated_at)
SELECT gen_random_uuid(), 'erp-free', 'Plan Free', 'BUSINESS', 0.00, 2, 5, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM billing.plan WHERE code = 'erp-free');

-- erp-free: todos los modulos activos (asi cualquier rubro queda funcional gratis para probar).
INSERT INTO billing.plan_module (plan_id, module_code)
SELECT p.id, m.code
FROM billing.plan p
JOIN catalog.modules m ON m.is_active = TRUE
WHERE p.code = 'erp-free'
ON CONFLICT DO NOTHING;
