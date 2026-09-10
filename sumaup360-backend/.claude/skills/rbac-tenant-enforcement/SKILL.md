---
name: rbac-tenant-enforcement
description: >-
  Cómo forzar permiso + tenant_id en cada endpoint del backend SUMAUP360. Úsalo al crear
  controllers, servicios o consultas que manejen datos de un tenant, para garantizar autorización
  y aislamiento.
---

# rbac-tenant-enforcement

Cada operación responde dos preguntas: ¿tiene el permiso? ¿sobre el tenant correcto?

## En el endpoint

- Anotar con `@PreAuthorize("hasAuthority('<permiso>')")` usando una clave de permiso estable.
- Resolver el `tenant_id` del contexto (poblado por el filtro de auth), no del cuerpo del cliente.
- Verificar que el usuario tiene membresía vigente en ese tenant.

## En servicio y repositorio

- Toda consulta de datos de tenant filtra por `tenant_id`. Sin filtro, no se ejecuta.
- En escrituras, fijar el `tenant_id` del contexto; nunca aceptarlo del request.
- En lecturas por id, validar que el registro pertenece al `tenant_id` del contexto (evitar IDOR).

## Jerarquía

Backoffice (global) > Empresa (tenant) > Trabajadores (subconjunto). Un rol de menor nivel nunca
excede los permisos del superior ni cruza a otro tenant.

## Reglas

- El backend es la única fuente de verdad de roles/permisos/tenant.
- Un tenant jamás ve datos de otro: probarlo con pruebas de aislamiento (ver `backend-qa`).
- Si un endpoint no necesita tenant (p. ej. catálogos globales), documentarlo explícitamente.

Complementa `firebase-admin-verify` y la skill raíz `sumaup-auth-firebase-rbac`.
