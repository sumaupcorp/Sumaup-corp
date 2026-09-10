# SUMAUP360 — Ecosistema

Monorepo de carpetas (polyrepo: un git por proyecto) del ecosistema **SUMAUP360**, una
plataforma **fintech / legaltech peruana** con dos líneas de producto:

1. **Línea Personas** → App móvil (Flutter) para personas naturales e independientes:
   taxistas, repartidores (PedidosYa, Rappi, etc.), arrendadores, profesionales con RUC,
   recibos por honorarios (4ta categoría) y Nuevo RUS.
2. **Línea Negocios** → SaaS / ERP web para administrar negocios: boticas, farmacias,
   ferreterías, restaurantes (con sub-rubros: cevichería, pollería, chifa, pastelería…),
   bodegas, minimarkets, librerías, belleza, lavanderías.

3. **Línea Interna (Backoffice SUMAUP)** → plataforma interna para el personal de SUMAUP
   (gerencia, admin, contadores, desarrollo, soporte, logística): procesa los comprobantes
   que suben los usuarios de la app, da soporte y gestiona las licencias del SaaS ERP. Es
   cross-tenant y la cima del RBAC.

La **Landing web** es comercial y presenta las líneas de cliente (Personas y Negocios).

## Proyectos (carpetas hermanas)

| Carpeta | Producto | Stack | Estado |
|---|---|---|---|
| `sumaup360-reborn/` | Landing web comercial | Next.js 16, React 19, TS, Tailwind, shadcn, Framer Motion | ✅ Existe |
| `sumaup360-backend/` | Backend (autoridad central) | Java 21, Spring Boot, Maven, PostgreSQL, Flyway, Spring Security, JWT, Firebase Admin, OpenAPI | 🟡 Skeleton |
| `sumaup360-saas/` | Dashboard SaaS / ERP | Next.js, React, TS, Tailwind, shadcn, TanStack Query, Recharts | 🟡 Skeleton |
| `sumaup360-mobile/` | App móvil Personas | Flutter, Dart, Riverpod, GoRouter, Dio, Freezed, secure_storage | 🟡 Skeleton |
| `sumaup360-backoffice/` | Plataforma interna (staff SUMAUP) | Next.js, React, TS, Tailwind, shadcn, TanStack Query, Recharts | 🟡 Skeleton |

## Decisiones de arquitectura selladas (Fase 0)

1. **Auth:** un solo proyecto Firebase para todo el ecosistema. Firebase **solo prueba
   identidad**. El **backend Spring es la ÚNICA autoridad** de roles, permisos, tenant,
   empresa, sucursal, plan y membresía. La autorización se propaga vía **custom claims**.
   Ver `docs/06-auth-security.md` y la skill `sumaup-auth-firebase-rbac`.
2. **RBAC:** Backoffice → controla el ERP → el ERP controla a cada empresa (tenant) → cada
   empresa controla a sus trabajadores. Roles y permisos jerárquicos.
3. **Multi-tenant:** una sola base `sumaup360_db`, separada por **schemas** de dominio. El
   aislamiento de tenant se hace por columna `tenant_id` + filtros forzados (no schema por
   tenant todavía). Ver `docs/03-database-design.md`.
4. **Rubros sin duplicar código:** ERP core único + sistema de **módulos habilitados** por
   `businessType` / `vertical` / `plan`. Los rubros se configuran con catálogos y módulos,
   no con código duplicado. Ver `docs/04-saas-erp-modules.md`.
5. **Monolito modular** preparado para escalar; **sin microservicios** todavía.
6. **Repos:** polyrepo (un git por proyecto). El versionado es independiente por producto.

## Cómo trabajar

Trabajamos **por fases** (ver `docs/08-development-roadmap.md`) y **proyecto por proyecto**.
No se implementan módulos completos hasta que su fase esté activa. Primero estructura,
documentación y base arquitectónica.

## Sistema de agentes

- **Agente padre:** `.claude/agents/sumaup360-master-architect.md` — orquesta todo, divide
  por dominios, mantiene consistencia, evita duplicar reglas de negocio.
- **Transversales (raíz):** `product-domain-architect`, `postgres-data-architect`,
  `qa-release-architect`.
- **Por proyecto:** cada carpeta tiene su `.claude/agents/` con un **arquitecto sub-padre**
  + sub-agentes especialistas + `.claude/skills/`.

Detalle completo en `docs/07-agent-system.md`.
