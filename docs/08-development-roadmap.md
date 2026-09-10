# 08 — Roadmap de desarrollo (por fases)

Trabajo **por fases** y **proyecto por proyecto**. No se abre una fase sin cerrar lo que la
habilita. No se implementan módulos completos fuera de la fase activa.

## Fase 0 — Estructura base del ecosistema  ← (actual)
- Estructura de carpetas y polyrepo.
- Sistema de agentes (padre + transversales + sub-padres + sub-agentes + skills).
- Documentación (`docs/00`–`09`).
- Skeleton de backend Spring Boot + config PostgreSQL + Flyway `V1`/`V2`.
- Skeletons documentados de SaaS y Mobile con comandos de init.

## Fase 1 — Backend de identidad y RBAC  ✅ (hecho y verificado contra PostgreSQL 18)
- Integración Firebase Admin (verificación de idToken) + modo dev local (`X-Debug-Uid`).
- Usuarios (provisión desde Firebase), roles, permisos (`recurso:accion`), `@PreAuthorize`.
- Tenants, empresas, sucursales y membresía usuario↔tenant. Aislamiento por `tenant_id`
  resuelto desde el contexto (verificado). Migraciones `V1`–`V6`.
- RBAC tenant-scoped: rol `tenant-admin` por tenant, gestión de roles y de usuarios
  (trabajadores) del tenant, permisos del principal filtrados por tenant activo. Jerarquía
  Backoffice→Empresa→Trabajadores verificada end-to-end.
- Pendiente menor: CRUD update/delete; pruebas `mvn verify`.

## Fase 2 — Landing productiva
- Diagnóstico web conectado, captura de leads, chatbot básico.

## Fase 3 — SaaS Dashboard base
- Empresas, sucursales, rubros, sistema de módulos habilitados, membresías y planes (UI +
  consumo de backend).

## Fase 4 — ERP core  ✅ (hecho y verificado contra PostgreSQL 18)
- Productos, clientes, inventario (stock + movimientos), caja/POS (apertura/cierre) y ventas.
- Venta atómica: exige caja abierta, valida producto, descuenta stock con rollback si falta.
- Multi-tenant + RBAC por permiso en todos los endpoints. Migraciones `V7`–`V9`.
- Proveedores (CRUD) y reportes (resumen de ventas, top productos, stock bajo, ventas por
  sucursal) hechos y verificados.
- Modulo **document-templates**: plantillas configurables (A4/80mm/58mm), preview HTML y
  generacion de PDF en backend (Thymeleaf + OpenHTMLToPDF), series/correlativos y tablas
  preparadas para facturacion electronica (XML/CDR/QR/hash/SUNAT como TODO). Migracion `V10`.
- Pendiente menor: update/delete, pruebas, integracion SUNAT real.

## Fase 5 — Vertical restaurantes  ✅ (backend hecho y verificado)
- Mesas, comandas (kitchen_order) e items, cola de cocina con estados, y cobro que genera
  venta y libera la mesa. Aplica a cevichería/pollería/chifa/pastelería (mismo módulo).
- Módulo del ERP core, multi-tenant + RBAC. Migración `V11`. Cobro exige caja abierta.
- Pendiente: descuento de insumos por receta; UI; pruebas.

## Fase 6 — Otros rubros
- Ferretería, bodega, minimarket, librería, farmacia/botica (módulos y catálogos propios).
- API de **catálogos** lista (rubros, sub-rubros, módulos, monedas, tipos de doc, segmentos)
  vía `GET /api/v1/catalog/*` — base para selección de rubro/módulos. ✅
- Motor de **`enabledModules` por empresa** ✅: defaults(rubro) ± overrides(empresa). Endpoints
  `/api/v1/erp/companies/{id}/modules`. Migración `V12`. Restaurante habilita tables/kitchen/menu;
  minimarket solo core — mismo código, distinta config. Techo por plan = TODO Fase 8.

## Fase 7 — App móvil (Personas) + Backoffice (intake)
- **Backend Personas ✅ (hecho y verificado):** diagnóstico+recomendación de plan, perfil,
  ingresos, gastos, recibos (PENDING para Backoffice), alertas y resumen mensual. Schema
  `app`, scopeado por usuario (no multi-tenant), migración `V13`. Endpoints `/api/v1/app/*`.
- App Flutter (UI): pendiente. Backoffice intake (procesar recibos): Fase 8.
- App: onboarding, diagnóstico, ingresos, gastos, alertas, perfil, subir comprobantes (sobre
  el backend de Fase 1).
- Backoffice interno: login staff + RBAC, y la **cola de procesamiento de comprobantes** para
  que los contadores carguen en SUMAUP360 lo que sube la app. Es lo que hace usable el
  "subir comprobante" del MVP. Ver `10-backoffice-internal.md`.

## Fase 8 — Billing y licencias  ✅ (backend hecho y verificado)
- Planes (Personas y Negocios) y suscripciones/membresías. Migración `V14`.
- **Techo de módulos por plan** aplicado sobre enabledModules (cierra el hook de Fase 6):
  erp-basico deja solo core, erp-full habilita todo.
- **Backoffice**: procesa los recibos que sube la app (cierra el hook de Fase 7,
  PENDING→PROCESSED) y gestiona **licencias** de tenants (cross-tenant).
- Pendiente: pagos reales (pasarela), UI.

## Fase 9 — Auditoría, notificaciones, chatbot, seguridad, QA  ✅ (backend hecho y verificado)
- **Auditoría** transversal automática (interceptor sobre toda mutación) + consulta staff.
- **Notificaciones** in-app (por usuario) + envío staff.
- **Chatbot Suma** (conversaciones/mensajes; respuestas por reglas, TODO LLM real).
- Tests `mvn test` verdes. Migración `V15`.
- Pendiente: Firebase REAL (apagar dev-mode con service account), reportes avanzados,
  endurecimiento y despliegue a producción.

## Backend — estado
Fases 1, 4, 5, 6, 7, 8, 9 + módulo de plantillas de documentos = ✅ (verificadas contra
PostgreSQL 18, migraciones V1–V15). Pendiente: Firebase real, integración SUNAT, y la UI de
frontend (SaaS/landing) y mobile (Flutter).

## Dependencias clave

- Fase 1 (auth/backend) habilita SaaS (3+) y Mobile (7).
- ERP core (4) habilita verticales (5) y rubros (6).
- Billing (8) puede solaparse parcialmente con SaaS/Mobile una vez exista identidad.

## Criterio de cierre de fase

Definido por `qa-release-architect` (`docs` y skill): build/lint/analyze verdes, pruebas de
features críticas, RBAC/tenant verificados, migraciones limpias, documentación actualizada.
