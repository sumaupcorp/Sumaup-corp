---
name: sumaup-conventions
description: Convenciones del ecosistema SUMAUP360 — nombres de proyectos/carpetas, git polyrepo, commits, idioma, documentación, y reglas de UI heredadas de la landing. Úsalo al crear archivos, carpetas, ramas o commits en cualquier proyecto.
---

# SUMAUP360 — Convenciones

## Nombres

- Proyectos / carpetas raíz: `sumaup360-<area>` → `-reborn` (landing), `-backend`, `-saas`,
  `-mobile`.
- Agentes: `kebab-case` descriptivo por rol (`backend-auth-security`, `saas-module-system`).
- Skills: `kebab-case` (`sumaup-domain-model`).
- Backend Java: paquetes `com.sumaup360.<modulo>`, clases `PascalCase`.
- DB: `snake_case`, schemas por dominio, migraciones `V{n}__descripcion_clara.sql`.
- TS/React: componentes `PascalCase`, hooks `useX`, archivos de UI `PascalCase.tsx`,
  utilidades `kebab-case.ts`.
- Dart/Flutter: archivos `snake_case.dart`, clases `PascalCase`, features por carpeta.

## Git (polyrepo)

- Un repositorio por proyecto. No mezclar historiales.
- Ramas: `feat/<scope>`, `fix/<scope>`, `chore/<scope>`, `docs/<scope>`.
- Commits cortos y claros, en español, imperativo: "agrega módulo de inventario".
- Commit/push solo cuando el usuario lo pida.

## Documentación

- README en cada carpeta importante (qué es, estado, cómo se levanta, fase).
- Un cambio de arquitectura actualiza su `docs/` en el mismo paso.
- Convertir fechas relativas a absolutas en docs.

## Idioma y UI (heredado de la landing)

- Texto real en **español**. Sin lorem ipsum.
- **Sin emojis** en UI ni en contenido de producto.
- Sin imágenes externas: solo assets locales.
- Mascota **Suma** (mono azul) y chatbot Suma como hilo conductor del ecosistema.

## Alcance del trabajo

- Primero estructura, documentación y contratos; después implementación.
- No generar código masivo innecesario. No abrir módulos fuera de la fase activa.
- Antes de tocar varias partes: revisar impacto y pasar por el master architect.
