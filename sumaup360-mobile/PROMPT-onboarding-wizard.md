# Prompt — Rediseño del onboarding (wizard multi-paso) SUMAUP360 App

## Contexto
App Flutter `sumaup360-mobile/sumaup360_app` (Línea Personas), backend Spring `sumaup360-backend`.
Identidad en Firebase; el backend es la autoridad. El **tipo de cliente/rol es inmutable**: se
elige una sola vez al crear la cuenta y solo soporte lo cambia desde el backoffice
(`PersonAdminService.changeClientType`, `DiagnosisService.updateMe` ya ignora cambios de rol).
Español real, sin emojis, design system Suma (azul/celeste/blanco, `AppColors`/`AppText`/`AppSpacing`,
íconos vía `AppIcon` sobre Hugeicons). Cada pantalla con estados de carga/error.

## Objetivo
Reemplazar la pantalla única `complete_profile_screen.dart` (a la que se llega tras verificar el
correo) por un **wizard de onboarding de 3 pasos con barra de progreso** arriba. Al terminar, el
usuario entra directo al Home (aunque aún no haya validado SUNAT).

## Decisiones ya tomadas (aplicarlas)
1. **Tercer rol:** agregar `SERVICIOS_PROFESIONALES` al enum `ClientType` (backend). Habilita features
   free (chat IA, consulta SUNAT, diagnóstico) y su módulo premium propio (Recibos por Honorarios +
   Suspensión de 4ta, plan `serv-premium` S/ 14.90) — ver `PROMPT-servicios-profesionales-premium.md`.
   Ajustar cualquier lugar que asuma solo 2 roles.
2. **Fin del onboarding → Home directo.** Separar "onboarding completo" de "perfil completo": añadir
   flag `onboarding_completed` en `app.person_profile`. El splash enruta al Home si
   `onboardingCompleted == true`; conectar SUNAT/RUC pasa a ser un CTA opcional dentro de la app.
   `profileCompleted` (que exige RUC ACTIVO+HABIDO) se mantiene para desbloquear lo tributario.
3. **Nombre y apellido separados.** Agregar `first_name` / `last_name` a `PersonProfile`; mantener
   `AppUser.displayName` = "nombre apellido" (para Firebase y compatibilidad).

## Flujo detallado (móvil)
Wizard con **barra de progreso** visible en los 3 pasos (paso 1 de 3, 2 de 3, 3 de 3). Puede ser un
`PageView` controlado o rutas dedicadas; mantener el estado del wizard (nombre, apellido, teléfono,
país, rol) en un provider/notifier hasta el guardado final.

- **Paso 1 — Tus datos**
  - Arriba, **foto de perfil** (reusar `_AvatarPicker` de complete_profile: sube a Firebase Storage
    `profile/{uid}/` y guarda la downloadURL en `photoUrl`; también `user.updatePhotoURL`). Opcional.
  - Dos inputs **lado a lado** (Row con dos Expanded): "Nombre" y "Apellido". Prefill si ya existen.
  - Debajo, el **correo** de la cuenta como campo **no editable** (`enabled: false`), tomado de
    `authControllerProvider.currentUser?.email` (es el que usó para registrarse).
  - Botón **Continuar** (nombre y apellido requeridos; validar no vacíos).
- **Paso 2 — Tu teléfono (opcional)**
  - `PhoneField` con selector de país (reusar el existente). Es **opcional**.
  - Botones **Continuar** y **Omitir** (ambos avanzan; Omitir sin teléfono).
- **Paso 3 — ¿A qué te dedicas?**
  - Tres opciones seleccionables (tarjetas): **Taxista** (`TAXISTA`), **Repartidor de delivery**
    (`DELIVERY_PEYA`), **Servicios profesionales** (`SERVICIOS_PROFESIONALES`).
  - Al **seleccionar un rol** se ejecuta el guardado final: `ProfileRepository.updateMe(...)` con
    firstName, lastName, phone, countryCode, photoUrl, clientType, y se marca `onboardingCompleted`;
    luego `ref.invalidate(profileMeProvider)` y `context.go(Routes.home)`. Manejar loading/error.

## Cambios backend
- `app/enums/ClientType`: agregar `SERVICIOS_PROFESIONALES`.
- Migración Flyway nueva (V31+): `ALTER TABLE app.person_profile ADD COLUMN first_name VARCHAR(80),
  ADD COLUMN last_name VARCHAR(80), ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT false;`
- `PersonProfile`: campos `firstName`, `lastName`, `onboardingCompleted`.
- DTOs `ProfileMeDtos`: `ProfileMeResponse` y `UpdateProfileMeRequest` += `firstName`, `lastName`,
  `onboardingCompleted`. `DiagnosisService.getMe/updateMe`: leer/escribir los nuevos campos;
  componer `displayName = firstName + " " + lastName` cuando lleguen; **respetar la inmutabilidad
  del rol** (solo setear clientType si estaba null). Al recibir el guardado del paso 3, setear
  `onboardingCompleted = true`.
- `FeatureAccessService`: contemplar `SERVICIOS_PROFESIONALES` (solo free por ahora; sin precio
  premium hasta definir su plan). Revisar `premiumPlanFor`/gating que hoy asumen taxi/peya.
- Backoffice (`persons/page.tsx` y enum de opciones): incluir el tercer rol en el selector de
  cambio de rubro; `premiumPlanFor` debe manejar el caso sin plan premium definido.

## Móvil — implementación
- Nuevo feature `features/onboarding/` (slice vertical: presentation/application) con el wizard y su
  barra de progreso; o refactor de `complete_profile_screen`. Rutas nuevas en `Routes` si aplica.
- Ajustar el flujo post-verificación (`verify_email_screen` / `splash_screen`): tras verificar correo
  y si `!onboardingCompleted` → wizard; si `onboardingCompleted` → Home.
- `ProfileMe` model + `ProfileRepository.updateMe`: agregar `firstName`, `lastName`,
  `onboardingCompleted`.
- Reusar `AvatarPicker` si se quiere foto (opcional, no lo pidió el usuario para este flujo).

## Criterios de aceptación
- Un usuario nuevo, tras verificar correo, ve el wizard con barra de progreso (3 pasos).
- Paso 1: nombre y apellido en inputs frente a frente; correo visible y bloqueado.
- Paso 2: teléfono opcional con Omitir.
- Paso 3: al elegir rol se guarda todo y entra al Home; el rol queda fijo (no editable desde perfil).
- Backend compila (`mvn -DskipTests compile`), migración aplica, `flutter analyze` limpio, backoffice
  `tsc --noEmit` limpio.
- Sin emojis; textos en español; estados de carga/error presentes.
