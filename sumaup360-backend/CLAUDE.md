# CLAUDE.md — sumaup360-backend

Contexto técnico del backend. Lee primero el `CLAUDE.md` raíz (`../CLAUDE.md`) y la
documentación del ecosistema en `../docs/` (00-09). No dupliques aquí lo que ya vive ahí.

## Referencias obligatorias

- `../docs/02-backend-architecture.md` — arquitectura del monolito modular.
- `../docs/03-database-design.md` — schemas, tablas, multi-tenant por `tenant_id`.
- `../docs/04-saas-erp-modules.md` — módulos ERP y rubros.
- `../docs/06-auth-security.md` — Firebase + RBAC + tenant.
- `../docs/09-conventions.md` y skill raíz `sumaup-conventions`.
- Skills raíz: `../.claude/skills/sumaup-auth-firebase-rbac`, `sumaup-domain-model`,
  `sumaup-conventions`.

## Decisiones selladas

- Java 21, Spring Boot 3.4.x, Maven. Monolito modular, paquetes `com.sumaup360.<modulo>`.
- AUTH: un solo Firebase para identidad; el backend es la **única** autoridad de
  roles/permisos/tenant/membresía (custom claims). Firebase prueba quién eres; el backend
  decide qué puedes hacer.
- Multi-tenant por columna `tenant_id` sobre una sola base `sumaup360_db`.
- Schemas: `auth`, `tenant`, `app`, `erp`, `billing`, `audit`, `chatbot`, `catalog`,
  `notification`. Se crean **solo con Flyway**.
- Personas (`app`) y Negocios (`erp`) **no se mezclan**. Lo transversal se comparte.
- Rubros por configuración (catálogos + módulos habilitados), nunca código duplicado.
- Español real, sin emojis. Primero estructura/contratos, después implementación.

## Estructura de un módulo

`web` (controllers/DTOs) → `service` → `domain` (entidades/reglas) → `repository`.
Ver skill local `spring-module-blueprint`.

## Sub-agentes del backend

Definidos en `.claude/agents/`. Padre del backend: `backend-spring-architect` (delega en
auth-security, multitenant-rbac, data-flyway, erp-modules, app-personas, billing,
backoffice, qa).

## Plataforma interna (Backoffice)

El paquete `com.sumaup360.backoffice` da soporte a `sumaup360-backoffice` (interno, staff,
cross-tenant): intake/procesamiento de comprobantes (sobre `app.receipt`) y licenciamiento
(sobre `billing`). Tres identidades distintas: staff ≠ usuario de app ≠ trabajador de tenant.
Ver `../docs/10-backoffice-internal.md` y el agente `backend-backoffice`.

## Reglas de oro

- No implementar lógica masiva sin contrato/estructura previa.
- Cada endpoint fuerza permiso + `tenant_id` (ver skill `rbac-tenant-enforcement`).
- Migraciones Flyway inmutables; nunca editar una ya aplicada (ver `flyway-migration-rules`).
- Nada se integra sin `mvn verify` verde.
