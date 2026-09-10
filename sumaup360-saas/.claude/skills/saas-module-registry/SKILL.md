---
name: saas-module-registry
description: Cómo funciona el registro de módulos del dashboard y los enabledModules. Úsalo al construir el menú/navegación, mapear ModuleKey a rutas/iconos, o configurar el ERP core por rubro sin duplicar dashboards.
---

# Skill: registro de módulos y enabledModules

El dashboard es **uno solo, configurable por rubro**. El registro mapea cada módulo a su
metadata de UI; los `enabledModules` (del backend) deciden qué se muestra.

## Piezas

- `src/types/domain.ts` — `ModuleKey`, `EnabledModules`, `SessionContext`.
- `src/lib/module-registry.ts` — `MODULE_REGISTRY: Record<ModuleKey, ModuleMeta>` y
  `CORE_MODULES`.

## Flujo

```
backend → SessionContext.enabledModules (ModuleKey[])
frontend → enabledModules.map(key => MODULE_REGISTRY[key])
        → agrupar por group, ordenar → Sidebar
```

## Reglas

1. El frontend **no calcula** `enabledModules`; los recibe resueltos del backend.
2. Nunca renderizar un módulo cuya key no esté en `enabledModules`.
3. Añadir un módulo = nueva `ModuleKey` en `domain.ts` + entrada en `MODULE_REGISTRY`.
4. Añadir un rubro/vertical = activar módulos + catálogos, **no** clonar el dashboard.
5. Core ERP común: `company, branch, customer, product, inventory, sale, pos, invoice,
   supplier, report`. Extras por vertical (ej. `tables`, `batch-expiry`) solo si aplican.

## Referencias

`docs/MODULE_SYSTEM.md`, `../../docs/04-saas-erp-modules.md`, skill raíz `sumaup-domain-model`.
