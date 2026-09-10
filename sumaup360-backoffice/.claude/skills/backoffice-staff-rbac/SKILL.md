---
name: backoffice-staff-rbac
description: Modelo de roles staff del backoffice y cómo ocultar/deshabilitar UI por permiso sin confiar en el cliente. Úsalo al construir el menú, gatear acciones o crear pantallas de staff/roles.
---

# Skill — RBAC staff del backoffice

Modelo de roles **internos** y cómo la UI los refleja. La autoridad de autorización es el
backend; el gating de UI es solo experiencia de usuario.

## Distinción crítica

staff (interno) ≠ usuario de la app (Personas) ≠ trabajador de un tenant (Negocios).

## Roles (extensibles)

`gerencia · admin · contador · desarrollador · soporte · logistica`.

- gerencia: lectura de reportes/licencias, visión global.
- admin: super-admin; todo, incluye staff y roles.
- contador: comprobantes + apoyo a soporte.
- desarrollador: logs/auditoría/técnico.
- soporte: tickets y vista de usuarios de la app.
- logistica: tenants, planes y operación.

## Módulos

`dashboard · comprobantes · support · app-users · licenses · tenants · plans · staff · roles · audit · reports`.
Matriz rol×módulo: `docs/STAFF_RBAC.md`.

## Gating (UX, no seguridad)

- El menú se construye desde `enabledModules` y los permisos de `StaffSessionContext`.
- Helper de lectura `can(resource, action)` que solo consulta el contexto del backend.
- Ocultar/deshabilitar = UX. Toda operación se valida en el backend con el `idToken`.
- Ante discrepancia entre lo que muestra la UI y lo que permite el backend, manda el backend.

## Tipos

`src/types/staff.ts`: `StaffRole`, `StaffUser`, `StaffPermission`, `BackofficeModule`,
`ModuleAccess`, `ModulePermissionMatrix`, `StaffSessionContext`.
