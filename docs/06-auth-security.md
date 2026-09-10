# 06 — Autenticación y seguridad

Decisión sellada (Fase 0). Detalle operativo vivo en la skill `sumaup-auth-firebase-rbac`.

## Principio

**Firebase prueba la identidad. El backend Spring decide la autorización.**

- Un **solo proyecto Firebase** para App móvil + SaaS web (+ landing cuando integre login).
- El backend es la **única autoridad** de roles, permisos, tenant, empresa, sucursal, plan,
  membresía.

## Flujo

```
Cliente → login en Firebase → idToken (JWT)
Cliente → backend con  Authorization: Bearer <idToken>
Backend → verifica idToken con Firebase Admin SDK
Backend → resuelve identidad interna por firebase_uid (schema auth)
Backend → carga línea, tenant, roles, permisos, plan
Backend → autoriza (RBAC + filtro tenant) y responde
```

## Custom claims

- El backend escribe claims espejo (`line`, `tenantId`, `roles`) para enrutado/UI.
- La autorización final **siempre** se reevalúa en el backend contra la BD; el claim es una
  pista, no la verdad.

## RBAC

- `user → roles → permissions`; permisos `recurso:accion` (`sale:create`,
  `inventory:adjust`, `report:read`, `module:enable`).
- Jerarquía: Backoffice SUMAUP → Empresa (tenant) → Trabajadores.
- Spring Security: filtro de verificación Firebase + method security (`@PreAuthorize`).

## Multi-tenant

- Aislamiento por `tenant_id` en cada consulta de dominio multi-tenant.
- Un rol alto **no** habilita ver datos de otro tenant.
- Resolución de tenant por contexto del usuario (no por parámetro manipulable del cliente).

## Línea Personas vs Negocios

- Misma identidad/verificación; el backend mantiene contextos separados.
- Nunca se mezclan permisos de ERP con la app personal.

## Manejo de secretos

- Service account de Firebase Admin fuera del control de versiones (ruta/variable de
  entorno). Nunca commitear credenciales.
- Tokens en cliente: `flutter_secure_storage` (mobile); almacenamiento seguro/HTTP-only según
  corresponda (web).

## JWT interno (opcional)

- Por defecto se usa el idToken de Firebase verificado por request.
- Si se requiere sesión propia, el backend emite JWT interno tras verificar Firebase. Una
  sola política, documentada aquí antes de implementarla.

## Checklist de seguridad por endpoint

1. ¿Verifica idToken? 2. ¿Exige permiso concreto? 3. ¿Filtra por `tenant_id`?
4. ¿Audita si es sensible? 5. ¿No expone datos de otra línea/tenant?
