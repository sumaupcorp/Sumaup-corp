-- Asignacion operativa del trabajador dentro del tenant:
-- - branch_id: sede asignada (null = todas; el dueño/administrador no se restringe).
--   Con sede asignada, el trabajador solo puede operar (vender/abrir caja) en ella.
-- - allowed_modules: JSON array de modulos visibles para ese usuario (null = todos los
--   habilitados). Es recorte de UI/UX; la seguridad real siguen siendo los permisos RBAC.
alter table tenant.membership
    add column branch_id uuid,
    add column allowed_modules text;
