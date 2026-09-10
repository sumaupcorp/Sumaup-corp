# CLAUDE.md — Ecosistema SUMAUP360 (raíz)

Este es el contexto **del ecosistema completo**. Cada proyecto tiene además su propio
`CLAUDE.md` con detalle técnico. Lee primero este, luego el del proyecto en el que trabajas.

## Qué es SUMAUP360

Ecosistema **fintech / legaltech peruano** con tres líneas que **NO se mezclan**:

- **Línea Personas (App móvil):** personas naturales e independientes — taxistas,
  repartidores de delivery, arrendadores, profesionales con RUC, recibos por honorarios,
  Nuevo RUS. Foco: orden tributario personal, ingresos, gastos, alertas, recomendación de
  plan.
- **Línea Negocios (SaaS / ERP):** administración de negocios con sucursales, trabajadores,
  roles, inventario, ventas, caja/POS. Rubros con sub-rubros (restaurantes → cevichería,
  pollería, chifa, pastelería; salud → botica, farmacia; etc.).
- **Línea Interna (Backoffice SUMAUP):** plataforma interna para el personal de SUMAUP
  (staff). Cross-tenant y cima del RBAC: procesa los comprobantes que suben los usuarios de
  la app, da soporte y gestiona las licencias del SaaS ERP. Roles staff: gerencia, admin,
  contador, desarrollador, soporte, logística. Ver `docs/10-backoffice-internal.md`.
  **Tres identidades distintas:** staff (interno) ≠ usuario de la app (Personas) ≠ trabajador
  de tenant (Negocios).

La **mascota Suma** (mono azul) y el **chatbot Suma** son transversales a todo el ecosistema.

## Regla de oro: no duplicar reglas de negocio

- Lo **transversal** vive en el backend y se comparte: `auth`, `users`, `roles`,
  `permissions`, `billing`, `notifications`, `audit`, `chatbot`, `catalog`.
- La **App móvil** y el **ERP** son líneas distintas: no mezclar su lógica de dominio.
- Los **rubros** NO duplican el sistema: extienden el ERP core con catálogos + módulos
  habilitados + configuración. Antes de crear algo "para un rubro", pregúntate si es
  configuración del core o realmente código nuevo.

## Antes de tocar varias partes: revisar impacto

Cualquier cambio que afecte el contrato entre backend ↔ frontend ↔ mobile debe:
1. Revisarse contra `docs/01-architecture-overview.md` y `06-auth-security.md`.
2. Pasar por el `sumaup360-master-architect` para coordinar dominios.
3. Actualizar la documentación afectada en `docs/` en el mismo cambio.

## Estado de los proyectos

- `sumaup360-reborn/` — Landing (✅ existe, tiene su propio CLAUDE.md y agentes). **No tocar**
  salvo que la tarea sea de la landing.
- `sumaup360-backend/` — Spring Boot (🟡 skeleton).
- `sumaup360-saas/` — Dashboard SaaS/ERP (🟡 skeleton).
- `sumaup360-mobile/` — Flutter (🟡 skeleton).
- `sumaup360-backoffice/` — Plataforma interna staff (🟡 skeleton).

## Stack por proyecto

- **Backend:** Java 21, Spring Boot, Maven, PostgreSQL, Flyway, Spring Security, JWT,
  Firebase Admin SDK, Swagger/OpenAPI. Monolito modular.
- **Landing y SaaS:** Next.js, React, TypeScript, Tailwind, shadcn/ui. SaaS añade TanStack
  Query + Recharts.
- **Mobile:** Flutter, Dart, Riverpod, GoRouter, Dio, Freezed, json_serializable,
  flutter_secure_storage.

## Base de datos

Una sola base `sumaup360_db` con schemas por dominio: `auth`, `tenant`, `app`, `erp`,
`billing`, `audit`, `chatbot`, `catalog`, `notification`. Schemas y tablas se crean **desde
el backend con Flyway**, nunca a mano. Ver `docs/03-database-design.md`.

## Convenciones

Ver `docs/09-conventions.md` y la skill `sumaup-conventions`. Resumen:
- Carpetas y proyectos: `sumaup360-<area>`.
- Sin emojis en UI ni en código; texto real en español (regla heredada de la landing).
- README en cada carpeta importante. No generar código masivo innecesario.

## Sistema de agentes

Ver `docs/07-agent-system.md`. Agente padre: `sumaup360-master-architect`.
