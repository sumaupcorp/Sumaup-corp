---
name: backoffice-design-system
description: Lenguaje visual del backoffice heredado de la landing y el SaaS (azul/celeste/blanco, Manrope/Sora, shadcn, sin emojis). Úsalo al crear cualquier pantalla, componente o estilo del backoffice.
---

# Skill — Design system Suma (backoffice)

El backoffice usa el **mismo** lenguaje visual que la landing y el SaaS (`sumaup360-saas`).
Reutilizar patrones, no reinventarlos.

## Reglas heredadas (no negociables)

- Español real, sin lorem ipsum. **Sin emojis** en UI ni contenido.
- Sin imágenes externas: solo assets locales.
- Fondo blanco. **Sin modo oscuro** ni theme switcher.
- Tipografía: **Manrope** (cuerpo/UI), **Sora** (titulares).
- Paleta **azul / celeste / blanco** de la landing.
- Mascota **Suma** (mono azul) y chatbot Suma como hilo conductor.
- Responsive mobile-first: 360 / 390 / 768 / 1280. Cero overflow horizontal.

## Componentes

- Base con **shadcn/ui** (`components/ui`) ajustados a la paleta Suma.
- Tablas, formularios y charts (Recharts) reutilizables y parametrizables.
- Layout con sidebar construido desde los módulos habilitados por el backend.

## Estados

Toda UI que consume datos muestra estados de **carga / error / vacío**.

## Referencias

`sumaup-conventions` (raíz), CLAUDE.md del proyecto, y para alinear visualmente
`../sumaup360-saas/CLAUDE.md`.
