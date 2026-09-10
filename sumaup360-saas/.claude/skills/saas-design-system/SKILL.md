---
name: saas-design-system
description: Reusa el lenguaje visual de la landing en el dashboard — azul/celeste/blanco, Manrope/Sora, shadcn/ui, sin emojis ni modo oscuro. Úsalo al crear o ajustar cualquier componente de UI del SaaS para mantener consistencia de marca.
---

# Skill: design system del dashboard

El dashboard hereda el lenguaje visual de la landing `sumaup360-reborn`. No reinventar;
reutilizar. NO tocar la landing.

## Paleta (tokens de marca, como en la landing)

- Negro corporativo `#05070D` (texto principal / foreground).
- Azul corporativo `#0B5BFF` (primary).
- Azul eléctrico `#1E8FFF` (ring) · Celeste `#33D1FF` (sky).
- Blanco `#FFFFFF` (background) · Fondo alterno `#F8FAFC` (muted/secondary).
- Texto secundario `#64748B` · Borde claro `#E2E8F0` · Acento `#EAF2FF`.

## Tipografía

- **Manrope** — cuerpo, UI, párrafos.
- **Sora** — H1 y titulares grandes.

## Reglas obligatorias

1. Fondo blanco puro. **Sin modo oscuro** ni theme switcher.
2. **Sin emojis.** Español real, sin lorem ipsum.
3. **Sin imágenes externas** (URLs/CDN de terceros): solo assets locales en `public/`.
4. Mascota Suma (mono azul) y chatbot Suma como hilo conductor.
5. Mobile-first: 360 / 390 / 768 / 1280, cero overflow horizontal.
6. Componentes con shadcn/ui; iconos lucide-react; gráficos con Recharts.

## Referencias

`../../../sumaup360-reborn/CLAUDE.md` (NO modificar la landing), `../../docs/09-conventions.md`,
agente `saas-ui-builder`.
