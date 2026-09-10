# features/tenants — Multi-tenant

Gestión del contexto de tenant en la UI: selección/visualización del tenant activo. El
aislamiento real por `tenant_id` lo hace el backend en cada consulta; el frontend solo refleja
el tenant resuelto en la sesión. Un rol alto no habilita ver datos de otro tenant.
