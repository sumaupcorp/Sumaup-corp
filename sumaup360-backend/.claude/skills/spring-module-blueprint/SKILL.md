---
name: spring-module-blueprint
description: >-
  Cómo estructurar un módulo del backend SUMAUP360 (paquete com.sumaup360.<modulo>) en capas
  web > service > domain > repository, con sus convenciones. Úsalo al crear o reorganizar
  cualquier módulo del backend.
---

# spring-module-blueprint

Estructura estándar de un módulo. Cada módulo es un paquete bajo `com.sumaup360`.

## Capas (dependencia hacia adentro)

```
<modulo>/
  web/         Controllers REST + DTOs (request/response). Anotaciones OpenAPI.
  service/     Casos de uso y reglas de aplicación. Transacciones aquí.
  domain/      Entidades JPA y reglas de dominio. Sin dependencias de web.
  repository/  Interfaces Spring Data JPA.
```

Regla de dependencias: `web → service → domain ← repository`. El dominio no conoce a web.

## Convenciones

- Paquete `com.sumaup360.<modulo>`; clases `PascalCase`.
- Controllers exponen DTOs, nunca entidades. Validación con Jakarta Bean Validation.
- La entidad declara su `@Table(schema = "...")` correcto (auth, app, erp, ...).
- Cada endpoint protegido con `@PreAuthorize` (permiso) y filtra por `tenant_id`.
- Servicios anotados `@Transactional` donde corresponda; `open-in-view` está desactivado.

## Qué NO hacer

- No poner lógica de negocio en controllers ni en repositorios.
- No duplicar reglas transversales (auth, billing, catalog): consumirlas.
- No mezclar dominio de `app` (Personas) con `erp` (Negocios).
