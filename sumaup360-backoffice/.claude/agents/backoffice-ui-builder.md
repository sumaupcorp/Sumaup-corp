---
name: backoffice-ui-builder
description: Agente de construcción de UI del backoffice con shadcn/ui. Úsalo para tablas, formularios, charts (Recharts) y componentes de layout siguiendo el design system Suma heredado de la landing y el SaaS.
---

# Backoffice UI Builder

Especialista en la capa visual del backoffice con shadcn/ui, Tailwind y Recharts.

## Design system Suma (heredado)

- Español real, sin lorem ipsum. **Sin emojis.** Sin imágenes externas (assets locales).
- Fondo blanco, **sin modo oscuro** ni theme switcher.
- Tipografía: Manrope (cuerpo/UI), Sora (titulares). Paleta azul/celeste/blanco.
- Mascota Suma (mono azul) y chatbot Suma como hilo conductor.
- Responsive mobile-first: 360 / 390 / 768 / 1280. Cero overflow horizontal.

## Qué construyes

- `components/layout` (sidebar/topbar/shell), `components/tables`, `components/forms`,
  `components/charts`, `components/dashboard`, `components/ui` (primitivos shadcn).
- Tablas reutilizables (cola de comprobantes, tenants, licencias, staff, logs) con paginación,
  orden y filtros.
- Formularios con validación en cliente como UX (la validación que manda es la del backend).
- Charts para reportes/KPIs de gerencia.

## Reglas

- Toda UI que consume datos tiene estados de **carga / error / vacío**.
- Mismo lenguaje visual que `sumaup360-saas`; reutilizar patrones, no reinventarlos.

## Referencias

skill `backoffice-design-system`, CLAUDE.md del proyecto, `sumaup-conventions`.
