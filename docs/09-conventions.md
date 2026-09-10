# 09 — Convenciones

Resumen operativo. Versión "viva" en la skill `sumaup-conventions`.

## Nombres

- Proyectos/carpetas raíz: `sumaup360-<area>` (`-reborn`, `-backend`, `-saas`, `-mobile`).
- Agentes y skills: `kebab-case` por rol/tema.
- Backend: paquetes `com.sumaup360.<modulo>`, clases `PascalCase`.
- DB: `snake_case`, schemas por dominio, migraciones `V{n}__descripcion.sql`.
- TS/React: componentes `PascalCase.tsx`, hooks `useX`, utils `kebab-case.ts`.
- Dart: archivos `snake_case.dart`, clases `PascalCase`, features por carpeta.

## Git (polyrepo)

- Un repo por proyecto. Ramas `feat|fix|chore|docs/<scope>`. Commits en español, imperativo.
- Commit/push solo cuando el usuario lo pida.

## Documentación

- README en cada carpeta importante. Un cambio de arquitectura actualiza su `docs/`.
- Fechas absolutas, no relativas.

## UI / contenido (heredado de la landing)

- Español real, sin lorem ipsum. Sin emojis. Sin imágenes externas (assets locales).
- Mascota Suma (mono azul) y chatbot Suma como hilo conductor.
- Fondo blanco, sin modo oscuro (aplica a web; mobile mantiene identidad de marca).

## Código

- Primero estructura/contratos, después implementación. No generar código masivo innecesario.
- Estados de carga/error/vacío en toda UI que consuma datos.
- No duplicar reglas de negocio; lo transversal vive en el backend.

## Calidad (ver 08 y qa-release-architect)

- Nada se integra sin build + lint/analyze verdes.
- Features críticas (auth, RBAC/tenant, billing, POS, diagnóstico) con pruebas.
