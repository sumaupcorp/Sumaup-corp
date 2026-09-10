---
name: flutter-dio-auth
description: Configuracion de Dio con interceptor que inyecta el Bearer idToken de Firebase y maneja expiracion/refresh. Usalo al tocar core/network, la capa data o cualquier llamada autenticada al backend.
---

# flutter-dio-auth

Como `sumaup360_app` se autentica contra el backend en cada request.

## Flujo (sellado)

1. Login en **Firebase Auth** -> `User` -> `idToken`.
2. `idToken` (y refresh) guardados en **flutter_secure_storage** (`core/storage/`).
3. El **interceptor Dio** agrega `Authorization: Bearer <idToken>` a toda request.
4. El **backend Spring** verifica el idToken y resuelve RBAC/membresia. La app **no** decide permisos.

## Interceptor (esquema)

```dart
// onRequest: leer idToken de secure storage y setear el header.
options.headers['Authorization'] = 'Bearer $idToken';

// onError: si responde 401 (token expirado):
//   1) refrescar: final fresh = await FirebaseAuth.instance.currentUser?.getIdToken(true);
//   2) guardar el nuevo token en secure storage.
//   3) reintentar la request original una sola vez.
//   4) si vuelve a fallar -> propagar error y forzar logout (limpiar storage + estado).
```

## Reglas

- Un unico cliente Dio compartido (`core/network/`), baseUrl por entorno (`core/config/`).
- Evitar tormenta de refresh: serializar/coalescer el refresh concurrente (un solo refresh en vuelo).
- Nunca loggear tokens. Tokens solo en secure storage, jamas en SharedPreferences o el repo.
- Mapear errores a `Failure`/`AppException` (`core/errors/`) con mensajes en espanol.
- 401 tras refresh fallido -> logout; 403 -> el backend nego permiso (no reintentar).

## Referencia

- Skill raiz `../../../.claude/skills/sumaup-auth-firebase-rbac`. Doc raiz `../../docs/06-auth-security.md`.
