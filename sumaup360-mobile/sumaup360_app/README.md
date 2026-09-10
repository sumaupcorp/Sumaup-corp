# sumaup360_app

App movil de la **Linea Personas** de SUMAUP360 (proyecto `sumaup360-mobile`).
Es la cara movil para personas naturales e independientes. **No** contiene logica de
ERP ni de empresas: esa vive en el backend y en el SaaS.

Segmentos: `taxi`, `delivery`, `landlord`, `professional-ruc`, `honorarios-4ta`, `nuevo-rus`.

## Stack

Flutter - Dart 3 - Riverpod - GoRouter - Dio - Freezed - json_serializable -
flutter_secure_storage - firebase_core - firebase_auth.

## Relacion con el backend e idToken

- La identidad se resuelve con **Firebase Auth**. La app hace login en Firebase y
  obtiene un **idToken**.
- Ese idToken se envia en **cada request** al backend mediante un interceptor Dio
  (`Authorization: Bearer <idToken>`). Ver `lib/core/network/`.
- El **backend Spring es la autoridad** de RBAC, contexto de Personas y membresia.
  La app **no decide permisos**; solo refleja lo que el backend autoriza.
- Tokens guardados en `flutter_secure_storage` (nunca en SharedPreferences ni en el repo).

## Comandos de init (NO ejecutados aun)

Este repositorio es un **skeleton documentado** (Fase 0). El SDK de Flutter puede no
estar instalado. Para materializar el proyecto:

```bash
# 1. Crear el proyecto Flutter dentro de esta carpeta (reusa el nombre del paquete)
flutter create --org com.sumaup360 --project-name sumaup360_app .

# 2. Agregar dependencias (ver pubspec.yaml para el detalle/versiones)
flutter pub add flutter_riverpod go_router dio freezed_annotation json_annotation \
  flutter_secure_storage firebase_core firebase_auth
flutter pub add --dev build_runner freezed json_serializable flutter_lints

# 3. Generar codigo (Freezed / json_serializable)
dart run build_runner build --delete-conflicting-outputs

# 4. Configurar Firebase (genera firebase_options.dart, no versionar secretos)
flutterfire configure

# 5. Verificar
flutter analyze && flutter test
```

> Los archivos `google-services.json`, `GoogleService-Info.plist` y `.env` **no** se versionan.

## Estructura

```
lib/
  app/        bootstrap, router GoRouter, providers globales, tema
  core/       config, constants, errors, network (Dio+auth), storage, theme, utils
  features/   slice vertical por feature: presentation / application / domain / data
  shared/     widgets, models y services reutilizables
test/         unit, widget e integracion
```

## Fase

Fase 0: estructura + contratos + documentacion. La construccion real ocurre en la
**Fase 7** del roadmap, despues del backend de auth (Fase 1). Ver
`../../docs/08-development-roadmap.md` y `../../docs/05-mobile-app-scope.md`.

## Documentacion y skills de referencia

- Docs raiz: `../../docs/` (en especial `05-mobile-app-scope.md`, `06-auth-security.md`,
  `09-conventions.md`).
- Skills raiz: `../../.claude/skills/` (`sumaup-auth-firebase-rbac`, `sumaup-conventions`,
  `sumaup-domain-model`).
- Skills y agentes propios de la app: `.claude/`.
