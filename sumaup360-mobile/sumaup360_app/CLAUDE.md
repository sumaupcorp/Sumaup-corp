# CLAUDE.md — sumaup360_app

Contexto tecnico para trabajar en la app movil (Linea Personas) de SUMAUP360.

## Que es

App Flutter para personas naturales e independientes. Solo cara movil de Personas;
**sin** logica de ERP/empresas. Segmentos: taxi, delivery, landlord, professional-ruc,
honorarios-4ta, nuevo-rus.

## Stack

Flutter / Dart 3 - Riverpod (estado) - GoRouter (navegacion) - Dio (HTTP) -
Freezed + json_serializable (modelos/DTOs) - flutter_secure_storage (tokens) -
firebase_core + firebase_auth (identidad).

## Arquitectura: slice vertical por feature

Cada feature en `lib/features/<feature>/` con 4 capas:
- `presentation/` — screens y widgets; consume providers; sin red directa.
- `application/` — providers/notifiers Riverpod; estado y casos de uso.
- `domain/` — modelos Freezed y contratos de repositorio; puro Dart.
- `data/` — datasources y repos con Dio; mapeo de DTOs json_serializable.

Transversal: `core/` (config, constants, errors, network, storage, theme, utils),
`shared/` (widgets, models, services) y `app/` (bootstrap, router, providers, tema).

## Auth (regla sellada)

- Login en **Firebase** -> `idToken`. Guardado en `flutter_secure_storage`.
- Interceptor Dio inyecta `Authorization: Bearer <idToken>` en cada request.
- El **backend** es la autoridad de RBAC/membresia/contexto. La app **no** decide permisos.
- Manejo de expiracion/refresh del idToken en el interceptor. Ver skill `flutter-dio-auth`.

## Reglas

- Espanol real en UI y codigo de cara al usuario. **Sin emojis**.
- Identidad de marca Suma (mono azul); paleta azul/celeste/blanco. Ver `flutter-design-system`.
- Cada pantalla con estados de carga / error / vacio.
- Sin logica de negocio critica en el cliente; el backend manda.
- Secrets fuera del repo (.env, google-services.json, GoogleService-Info.plist).
- Dart: archivos `snake_case.dart`, clases `PascalCase`.
- Codigo generado (`*.g.dart`, `*.freezed.dart`) **no** se versiona.

## Referencias (no duplicar)

- Docs raiz: `../../docs/` (`05-mobile-app-scope.md`, `06-auth-security.md`, `09-conventions.md`).
- Skills raiz: `../../.claude/skills/` (`sumaup-auth-firebase-rbac`, `sumaup-conventions`,
  `sumaup-domain-model`).
- Agentes y skills propios: `.claude/agents/` y `.claude/skills/`.
