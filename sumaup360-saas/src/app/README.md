# src/app — Rutas (Next.js App Router)

Rutas del dashboard organizadas con grupos de rutas. Las páginas de módulo se renderizan
según los `enabledModules` del backend (ver `../../docs/MODULE_SYSTEM.md`).

## Grupos de rutas previstos

- `(auth)/` — flujo de identidad. Ej. `login`. Sin sidebar; layout mínimo. Usa Firebase para
  obtener el idToken; el backend resuelve la sesión.
- `(dashboard)/` — área autenticada con `layout.tsx` (sidebar + topbar). Aquí viven las rutas
  por módulo: `empresa`, `sucursales`, `clientes`, `productos`, `inventario`, `ventas`, `pos`,
  `comprobantes`, `proveedores`, `reportes`, y extras por vertical (`mesas`, `comandas`, etc.).

## Reglas

- Una ruta de módulo solo es accesible si su `ModuleKey` está en `enabledModules`.
- El `layout.tsx` de `(dashboard)` construye el menú desde el registro de módulos.
- Toda página que consume datos implementa estados de carga / error / vacío.
- Fondo blanco, sin modo oscuro. Español real, sin emojis.
