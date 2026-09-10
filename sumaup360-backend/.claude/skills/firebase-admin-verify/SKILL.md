---
name: firebase-admin-verify
description: >-
  Cómo verificar el idToken de Firebase con el Admin SDK y construir el SecurityContext del
  backend SUMAUP360. Úsalo al implementar el filtro de autenticación o cualquier integración con
  Firebase Admin.
---

# firebase-admin-verify

Firebase prueba la identidad; el backend decide la autorización. Este skill cubre la verificación.

## Inicialización

- `FirebaseApp` se inicializa una vez (en `config/`) con el service account de
  `firebase.service-account` (variable `FIREBASE_SERVICE_ACCOUNT`). Nunca versionar el JSON.

## Flujo de verificación (por petición)

1. Leer el header `Authorization: Bearer <idToken>`.
2. `FirebaseAuth.getInstance().verifyIdToken(idToken)` → obtener `uid` (y email).
3. Resolver el usuario del backend por ese `uid` (schema `auth`).
4. Cargar roles, permisos y `tenant_id` efectivos **desde el backend** (no desde claims del
   cliente).
5. Construir `Authentication` con las autoridades y poblar el `SecurityContext`.

## Reglas

- La autoridad de roles/permisos/tenant es el backend; el idToken solo prueba quién es.
- Token inválido/expirado → 401. Identidad válida sin permiso → 403.
- No confiar en custom claims escritos por el cliente; los claims los gobierna el backend.
- Endpoints públicos (Swagger, health) quedan fuera del filtro.

Complementa `rbac-tenant-enforcement` (la decisión de autorización) y `sumaup-auth-firebase-rbac`.
