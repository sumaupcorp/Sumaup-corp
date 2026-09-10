# STAFF_RBAC — Roles staff y matriz rol × módulo

RBAC **interno** del backoffice. Define qué módulo ve cada rol staff. Es **extensible**:
el backend puede añadir roles/permisos. La UI solo refleja; **no decide** autorización.

> Recordatorio: staff (interno) ≠ usuarios de la app (Personas) ≠ trabajadores de un tenant
> (Negocios). Este documento trata SOLO de roles staff internos.

## Roles staff

| Rol            | Descripción                                                              |
|----------------|--------------------------------------------------------------------------|
| `gerencia`     | Dirección. Lectura de reportes/KPIs y licencias. Visión global.          |
| `admin`        | Super-admin. Todo, incluida administración de staff y roles.             |
| `contador`     | Procesa comprobantes y apoya soporte de la app móvil.                    |
| `desarrollador`| Soporte técnico: logs, auditoría, herramientas técnicas.                 |
| `soporte`      | Atención a usuarios de la app: tickets y vista de usuario.               |
| `logistica`    | Operación: tenants, planes y tareas operativas.                          |

## Módulos del backoffice

`dashboard · comprobantes · support · app-users · licenses · tenants · plans · staff · roles · audit · reports`

## Matriz rol × módulo

Leyenda: `F` total · `L` solo lectura · `—` sin acceso.

| Módulo        | gerencia | admin | contador | desarrollador | soporte | logistica |
|---------------|:--------:|:-----:|:--------:|:-------------:|:-------:|:---------:|
| dashboard     |    L     |   F   |    L     |       L       |    L    |     L     |
| comprobantes  |    L     |   F   |    F     |       —       |    L    |     —     |
| support       |    —     |   F   |    F     |       —       |    F    |     —     |
| app-users     |    L     |   F   |    L     |       L       |    F    |     —     |
| licenses      |    L     |   F   |    —     |       —       |    —    |     L     |
| tenants       |    L     |   F   |    —     |       L       |    —    |     F     |
| plans         |    L     |   F   |    —     |       —       |    —    |     L     |
| staff         |    —     |   F   |    —     |       —       |    —    |     —     |
| roles         |    —     |   F   |    —     |       —       |    —    |     —     |
| audit         |    L     |   F   |    —     |       F       |    —    |     —     |
| reports       |    F     |   F   |    L     |       L       |    —    |     L     |

> Esta matriz es una **referencia conceptual de UI**. Los permisos efectivos los entrega el
> backend en el contexto de sesión staff; ante discrepancia, manda el backend.

## Gating de UI (UX, no seguridad)

- El menú/sidebar se construye desde los permisos que entrega el backend.
- Ocultar o deshabilitar acciones por permiso es **experiencia de usuario**, nunca control de
  acceso real. Toda operación se valida en el backend con el `idToken`.
- Ver tipos en `src/types/staff.ts`, skill `backoffice-staff-rbac` y `../docs/06-auth-security.md`.
