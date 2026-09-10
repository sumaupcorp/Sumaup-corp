# src/app — Rutas (App Router)

Rutas del backoffice con Next.js App Router. Acceso restringido a staff.

- `(auth)/` — login de staff por Firebase (idToken). Sin sidebar.
- `(backoffice)/` — layout protegido con sidebar; el menú se construye desde los módulos que
  habilita el backend en la sesión staff.
- Rutas por módulo dentro de `(backoffice)/`: `dashboard`, `comprobantes`, `support`,
  `app-users`, `licenses`, `tenants`, `plans`, `staff`, `roles`, `audit`, `reports`.

La autorización real la decide el backend; estas rutas solo reflejan permisos.
