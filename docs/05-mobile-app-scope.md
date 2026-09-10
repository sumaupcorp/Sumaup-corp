# 05 — Alcance de la App móvil (Línea Personas)

Proyecto: `sumaup360-mobile`. Flutter · Dart · Riverpod · GoRouter · Dio · Freezed ·
json_serializable · flutter_secure_storage.

## Para quién

Personas naturales e independientes. Segmentos:
`taxi`, `delivery`, `landlord`, `professional-ruc`, `honorarios-4ta`, `nuevo-rus`.

La app **no** contiene lógica de ERP ni de empresas. Es la cara móvil de la Línea Personas.

## Features (carpetas en `lib/features/`)

| Feature | Qué hace |
|---|---|
| `onboarding` | Presentación, selección de segmento, valor de la app |
| `auth` | Login/registro con Firebase; guarda sesión en secure storage |
| `diagnosis` | Diagnóstico tributario inicial según segmento → recomendación de plan |
| `home` | Resumen: situación, próximos vencimientos, accesos rápidos |
| `income` | Registro y listado de ingresos |
| `expenses` | Registro y listado de gastos |
| `receipts` | Recibos por honorarios / comprobantes personales |
| `alerts` | Alertas tributarias y recordatorios |
| `plans` | Planes, recomendación, comparativa |
| `profile` | Datos del usuario, preferencias |
| `chatbot` | Suma (asistente) |
| `subscription` | Suscripción/membresía y pagos |

## Arquitectura interna

- **core/** — config, constantes, errores, network (Dio + interceptores de auth),
  storage (secure storage), theme, utils.
- **features/** — cada feature en slice vertical: `presentation` (screens/widgets),
  `application` (providers/notifiers Riverpod), `domain` (modelos Freezed), `data`
  (datasources/repos con Dio).
- **shared/** — widgets, models y services reutilizables.
- **app/** — bootstrap, router (GoRouter), inyección de providers, tema global.

## Integración con backend

- Auth: login en Firebase → idToken → se envía en cada request (interceptor Dio).
- El backend resuelve contexto de Personas y RBAC personal (la app no decide permisos).
- Modelos generados con Freezed/json_serializable a partir de los contratos OpenAPI.

## Reglas

- Mobile first real (es nativa): estados de carga/error/vacío en cada pantalla.
- Sin lógica de negocio crítica en el cliente; el backend manda.
- Sin emojis en UI; español; identidad visual coherente con Suma (mono azul).
- Secrets fuera del repo; `flutter_secure_storage` para tokens.

## Estado

Fase 0: estructura documentada + comandos de init. La app se construye en **Fase 7** del
roadmap (después del backend de auth, Fase 1). Ver `08-development-roadmap.md`.
