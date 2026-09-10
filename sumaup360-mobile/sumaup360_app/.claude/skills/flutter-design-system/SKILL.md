---
name: flutter-design-system
description: Sistema de diseno de marca Suma para sumaup360_app — colores, tipografia, componentes y reglas de UI. Usalo al construir tema, pantallas o widgets, o al revisar consistencia visual.
---

# flutter-design-system

Identidad visual de `sumaup360_app`, coherente con la marca SUMAUP360 y la mascota **Suma**
(mono azul). Centralizado en `core/theme/` y `shared/widgets/`.

## Marca

- Mascota y chatbot: **Suma**, mono azul. Tono cercano, claro y confiable.
- **Espanol real** en toda la UI. **Sin emojis** en ningun texto, boton o estado.

## Color

- Paleta: **azul** (primario), **celeste** (acento/secundario), **blanco** (superficie).
- Definir como tokens en `ThemeData` (`ColorScheme`); nunca colores hardcodeados sueltos.
- Soportar tema claro (y oscuro si aplica) desde el mismo set de tokens.

## Tipografia

- Una familia consistente; escala tipografica via `TextTheme`.
- Jerarquia clara: titulos, cuerpo, etiquetas. Buen contraste y legibilidad.

## Componentes

- Botones de marca, tarjetas, inputs, banners de alerta, avatar de Suma -> en `shared/widgets/`.
- Estados obligatorios en cada pantalla: **carga**, **error**, **vacio** (componentes reutilizables).
- Espaciado y radios consistentes (definir tokens de spacing/radius).

## Reglas

- Reutilizar tokens y widgets; no duplicar estilos.
- Mobile first real (app nativa): tactil, accesible, responsivo.
- Textos centralizados; nada de strings sueltos en widgets profundos.

## Referencia

- Skill raiz `sumaup-conventions` (UI heredada de la landing). Doc raiz `../../docs/00-product-vision.md`.
