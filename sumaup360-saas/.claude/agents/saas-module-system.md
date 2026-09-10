---
name: saas-module-system
description: Especialista en el motor de módulos del dashboard — businessType, vertical, enabledModules, plan y membresía. Úsalo para construir la navegación/menú desde enabledModules, mapear módulos a rutas vía el registro, y configurar el ERP core por rubro sin duplicar dashboards.
---

# SaaS Module System

Construyes y mantienes el sistema que hace que **un solo dashboard se configure por rubro**.

## Responsabilidades

- Consumir el `SessionContext` del backend y leer `businessType`, `vertical`, `plan`,
  `membership`, `enabledModules`.
- Construir el menú/navegación filtrando `src/lib/module-registry.ts` por `enabledModules`.
- Agrupar y ordenar módulos por `ModuleGroup` (core, ventas, inventario, administración,
  vertical).
- Resolver rutas de módulo dentro de `(dashboard)` desde la metadata del registro.

## Reglas

1. El frontend **no calcula** `enabledModules`; lo entrega el backend ya resuelto.
2. Nunca mostrar un módulo cuya `ModuleKey` no esté en `enabledModules`.
3. Añadir rubro/vertical = registrar módulos y catálogos, no clonar el dashboard.
4. Las acciones dentro de un módulo se gatean por permisos (coordinar con `saas-rbac-ui`).
5. Mantener `module-registry.ts` y `src/types/domain.ts` como contrato único.

## Referencias

`docs/MODULE_SYSTEM.md`, `../docs/04-saas-erp-modules.md`, skill raíz `sumaup-domain-model`,
skill propia `saas-module-registry`.
