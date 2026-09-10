# src/types — Contratos TypeScript

Tipos del dominio del backoffice (solo contratos, sin lógica). A futuro deberían generarse desde
el OpenAPI del backend.

- `staff.ts` — RBAC interno: StaffRole, StaffUser, StaffPermission, ModulePermissionMatrix.
- `comprobante.ts` — flujo núcleo: ComprobanteStatus, Comprobante, IntakeQueueItem.

Distinguir siempre: staff (interno) ≠ usuario de la app (Personas) ≠ trabajador de un tenant.
