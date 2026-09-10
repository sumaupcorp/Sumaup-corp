---
name: sumaup-auth-firebase-rbac
description: Arquitectura de autenticación y autorización de SUMAUP360 — un solo Firebase para identidad, backend Spring como autoridad de RBAC/tenant/membresía, custom claims, verificación de idToken y enforcement por tenant. Úsalo al tocar login, sesiones, roles, permisos, multi-tenant o seguridad en cualquier proyecto.
---

# SUMAUP360 — Auth & RBAC

Decisión sellada en Fase 0. No cambiar sin pasar por el `sumaup360-master-architect`.

## Principio central

**Firebase prueba QUIÉN eres. El backend decide QUÉ puedes hacer.**

- **Un solo proyecto Firebase** para todo el ecosistema (App móvil + SaaS web).
- Firebase Auth solo gestiona identidad (email/clave, social, OTP).
- El **backend Spring Boot es la ÚNICA autoridad** de roles, permisos, tenant, empresa,
  sucursal, plan y membresía.

## Flujo de autenticación

```
1. Cliente (mobile/web) inicia sesión en Firebase  → recibe Firebase idToken (JWT).
2. Cliente llama al backend con  Authorization: Bearer <idToken>.
3. Backend verifica el idToken con Firebase Admin SDK (firma, exp, audiencia).
4. Backend resuelve la identidad interna por firebase_uid (schema auth).
5. Backend carga contexto: línea de producto, tenant(s), empresa, rol(es), permisos, plan.
6. Backend autoriza la operación con su propio RBAC + filtro de tenant.
```

## Custom claims

- El backend escribe **custom claims** en Firebase (vía Admin SDK) como espejo ligero del
  contexto: ej. `line` (`person`|`business`), `tenantId`, `roles`. Sirven para enrutado/UI,
  **no** como fuente de autorización final.
- La **autorización real** siempre se reevalúa en el backend contra la BD, no se confía solo
  en el claim (el claim puede estar desactualizado).

## Línea Personas vs Negocios

- Mismo Firebase, misma verificación. El backend distingue la línea por el contexto del
  usuario (`line` claim + datos en `auth`/`app` vs `tenant`).
- Un usuario podría, en teoría, pertenecer a ambas líneas; el backend mantiene contextos
  separados y nunca mezcla permisos de ERP con la app personal.

## RBAC en el backend

- Modelo: `user → roles → permissions`. Permisos = `recurso:accion` (`sale:create`,
  `inventory:adjust`, `report:read`).
- Jerarquía: Backoffice SUMAUP → Empresa (tenant) → Trabajadores. (Ver `sumaup-domain-model`.)
- Enforcement: cada endpoint de Negocios valida **permiso + `tenant_id`** (y `branch_id` si
  aplica). Spring Security con method security + un filtro/intercepción de tenant.

## Multi-tenant

- Aislamiento por columna `tenant_id`. Todo query de un dominio multi-tenant lo filtra
  obligatoriamente. Nunca exponer datos de otro tenant aunque el rol parezca alto.

## JWT interno (opcional)

- Por defecto el backend trabaja con el idToken de Firebase verificado por request.
- Si se necesita sesión propia (refresh largo, claims pesados), el backend puede emitir un
  **JWT interno** tras verificar Firebase. Mantener una sola política y documentarla aquí.

## Qué NO hacer

- No confiar en el cliente para roles/tenant.
- No duplicar la lógica de auth en cada proyecto: el frontend/mobile solo envía el idToken.
- No usar dos proyectos Firebase (decisión sellada: uno solo).
