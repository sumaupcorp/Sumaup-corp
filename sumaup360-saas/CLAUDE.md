# CLAUDE.md — sumaup360-saas (Dashboard SaaS / ERP)

Contexto técnico de este proyecto. Lee primero `../CLAUDE.md` (ecosistema) y luego este.

## Qué es

Dashboard web de la **Línea Negocios**: administración multi-tenant de negocios
(empresa → sucursales → trabajadores). **Un solo dashboard que se configura por rubro.**
No existe un dashboard por rubro: hay un ERP core que se adapta con catálogos y módulos.

## Stack

Next.js (App Router) · React · TypeScript · Tailwind · shadcn/ui · TanStack Query · Recharts.

## Concepto central: módulos por rubro

- `businessType` (rubro) y `vertical` (sub-rubro) definen el conjunto base de módulos.
- `plan` y `membership` fijan el techo comercial (qué módulos y límites permite el contrato).
- `enabledModules` es el resultado efectivo que **entrega el backend**; la UI solo lo refleja.
- El **menú/navegación se construye desde `enabledModules`** (no se hardcodea por rubro).
- Tablas, KPIs y reportes son componentes **genéricos parametrizados por módulo**.

Detalle: `docs/MODULE_SYSTEM.md`, `src/lib/module-registry.ts`, skill `saas-module-registry`.

## Auth y RBAC (regla de oro)

- **Firebase = identidad. Backend = autoridad de RBAC, tenant y membresía.**
- El frontend solo envía `Authorization: Bearer <idToken>` y refleja permisos.
- El frontend **nunca** decide autorización ni reglas de negocio: ocultar/deshabilitar UI por
  permiso es UX, no seguridad. Ver `docs/06-auth-security.md` (raíz) y skill `saas-rbac-gating`.

## Reglas UI (heredadas de la landing)

- Español real, sin lorem ipsum. **Sin emojis.** Sin imágenes externas (assets locales).
- Fondo blanco, **sin modo oscuro** ni theme switcher.
- Mascota Suma (mono azul) y chatbot Suma como hilo conductor.
- Tipografía: Manrope (cuerpo/UI), Sora (titulares). Paleta azul/celeste/blanco de la landing.
- Responsive mobile-first: 360 / 390 / 768 / 1280. Cero overflow horizontal.
- Toda UI que consume datos tiene estados de **carga / error / vacío**.

## Estructura

- `src/app/` — rutas App Router: `(auth)` login, `(dashboard)` con layout y rutas por módulo.
- `src/components/` — `layout` (sidebar/topbar), `dashboard`, `modules`, `tables`, `forms`,
  `charts`, `ui` (shadcn).
- `src/features/` — lógica por dominio (auth, tenants, companies, branches, business-types,
  memberships, roles, permissions, pos, inventory, sales, products, customers, billing, reports).
- `src/lib/` — api client (interceptor idToken), query client, utils, `module-registry.ts`.
- `src/types/` — tipos de dominio (`domain.ts`).
- `src/data/` — datos estáticos/seed de UI.

## Referencias (no duplicar)

- Docs ecosistema: `../docs/00-09`. Skills raíz: `../.claude/skills/`.
- Agentes/skills propios: `.claude/agents/`, `.claude/skills/`.

## Reglas de trabajo

Primero estructura y contratos, después implementación. Nada se integra sin `build` + `lint`
verdes. No duplicar reglas de negocio; los rubros se configuran, no se programan por separado.
