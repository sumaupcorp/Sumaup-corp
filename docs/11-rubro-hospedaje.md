# 11 — Rubro Hotelería / Hospedaje: análisis funcional y TDR

Documento de referencia (TDR) para incorporar el rubro **hospedaje** (hoteles, hostales,
hospedajes, apart-hoteles, albergues) al SaaS ERP de SUMAUP360. Se usa como contrato de
alcance entre backend, SaaS y documentación. Estado: **aprobado para implementación, fase 1**.

---

## 1. Antecedentes y contexto

- El SaaS ERP funciona por **rubros** (`catalog.business_types`) que habilitan **módulos**
  (`catalog.modules` → `catalog.business_type_module` → `erp.company_module`), con techo por
  plan (`billing.plan_module`). Ver `docs/04-saas-erp-modules.md` y
  `sumaup360-saas/docs/MODULE_SYSTEM.md`.
- Regla de oro del ecosistema: un rubro **no duplica el sistema**; extiende el core con
  catálogos, módulos habilitados y configuración. Solo se escribe código nuevo cuando el
  dominio realmente lo exige.
- En el Perú los establecimientos de hospedaje se rigen por el **Reglamento de
  Establecimientos de Hospedaje (DS 001-2015-MINCETUR)**: clases hotel, apart-hotel, hostal
  y albergue, y obligación de llevar **registro de huéspedes** (ficha con documento de
  identidad). Esto define parte del alcance funcional.

## 2. Análisis funcional

### 2.1 Segmento objetivo

Hoteles pequeños y medianos, hostales, hospedajes familiares y casas de huéspedes que hoy
administran habitaciones en cuaderno o Excel: recepción con 1-3 personas, sin channel
manager, cobro en efectivo/Yape/Plin/tarjeta. Mismo perfil que el resto de rubros SUMAUP:
negocios reales peruanos, no cadenas.

### 2.2 Procesos del negocio a cubrir

1. **Definir la oferta**: tipos de habitación (simple, doble, matrimonial, suite...) con
   tarifa por noche y capacidad; habitaciones físicas numeradas por sucursal.
2. **Reservar**: registrar una reserva con huésped titular, habitación, fecha de entrada y
   salida, tarifa pactada. Evitar doble reserva (solape de fechas por habitación).
3. **Check-in**: llegada del huésped, registro de acompañantes (ficha de huéspedes),
   habitación pasa a ocupada.
4. **Estadía**: cargos a la habitación (minibar, lavandería, desayuno, servicios) que se
   acumulan en la cuenta de la estadía.
5. **Check-out**: liquidación — noches + cargos se cobran por el **POS existente** (venta
   normal: efectivo/tarjeta/Yape/Plin, ticket con serie/correlativo, boleta/factura si el
   módulo `e-invoicing` está activo). Habitación pasa a limpieza y luego a disponible.
6. **Operación diaria**: rack de habitaciones (quién está en cuál, qué está libre/sucia/en
   mantenimiento), llegadas y salidas del día, ocupación.

### 2.3 Decisión de diseño: por qué un módulo nuevo y no citas

El módulo `appointments` modela **franjas horarias de un día** (`scheduled_at` +
`duration_minutes`); el hospedaje modela **ocupación exclusiva de un recurso (habitación)
por un rango de noches**, con estados propios (reservada → check-in → check-out) y cuenta
corriente de consumos. Forzar citas rompería ambos dominios. Se crea el módulo genérico
**`lodging`** (clave única), reutilizable a futuro por otros negocios de recursos
alquilables (cocheras, canchas, coworking) si se generaliza.

### 2.4 Qué se reutiliza del core (sin código de dominio nuevo)

| Módulo core | Uso en hospedaje |
|---|---|
| `customer` | Huésped titular (documento, nombre, teléfono); su ficha acumula estadías y gasto |
| `pos` / `sale` | Cobro del check-out como venta normal: caja, arqueo, métodos de pago, ticket, WhatsApp |
| `product` / `inventory` | Productos y servicios cargables a la habitación (minibar, lavandería, desayuno) |
| `invoice` + `e-invoicing` | Boleta/factura del hospedaje cuando el negocio active facturación electrónica |
| `report` | KPIs de ventas ya existentes; se suman KPIs de ocupación (fase 1, sección RF-7) |
| `branch` / `users` | Multi-sede y trabajadores con sede asignada (V48) aplican tal cual |
| `tables`/`kitchen`/`menu` | Si el hotel tiene restaurante, se activan con la pregunta de operación existente |

### 2.5 Qué NO entra (fuera de alcance de fase 1)

- Reserva online pública de habitaciones (se hará en fase 2 reutilizando el patrón
  `booking_page`/token/QR de citas, con calendario de disponibilidad).
- Channel manager / OTAs (Booking, Airbnb), motor de tarifas dinámicas, temporadas.
- Exoneración de IGV a no domiciliados (DL 919: pasaporte + TAM) — se documenta como
  requisito de la fase SUNAT/NubeFact, no de este módulo.
- Housekeeping avanzado (asignación de personal de limpieza, checklists); fase 1 solo
  maneja el estado LIMPIEZA de la habitación.
- Reporte automático a MINCETUR; fase 1 solo garantiza tener los datos del registro de
  huéspedes exportables.

## 3. TDR — Términos de referencia

### 3.1 Objetivo

Incorporar el rubro hospedaje al SaaS ERP: alta del rubro y sus sub-rubros en el catálogo,
módulo nuevo `lodging` end-to-end (backend + SaaS), integrado con clientes, POS, planes,
RBAC y onboarding, sin duplicar reglas de negocio del core.

### 3.2 Catálogo (rubro, sub-rubros, módulo)

- **Rubro**: `catalog.business_types` → code **`lodging`**, name **`Hotel / Hospedaje`**.
- **Sub-rubros** (`catalog.verticals`, patrón de `restaurant`): `hotel` (Hotel),
  `hostal` (Hostal), `apart-hotel` (Apart-hotel), `albergue` (Albergue),
  `hospedaje` (Hospedaje / Casa de huéspedes).
- **Módulo nuevo**: `catalog.modules` → code **`lodging`**, name
  **`Habitaciones y hospedaje`**, `is_core = FALSE`.
- **Defaults del rubro** (`catalog.business_type_module`): los 11 módulos core + `lodging`.
  `tables`/`kitchen`/`menu` NO por defecto (se activan por pregunta de operación).
- **Planes** (`billing.plan_module`): sumar `lodging` a `erp-free` y `erp-full`
  (gotcha V41: módulo que no está en el plan queda capado e indescubrible).

### 3.3 Modelo de datos (schema `erp`, una migración: `V54__lodging_module.sql`)

Todas las tablas con `tenant_id` + `company_id` (aislamiento por empresa, patrón V47) y
las operativas además con `branch_id`. Nombres en snake_case, UUID PK, `created_at`/
`updated_at` TIMESTAMPTZ, idempotencia IF NOT EXISTS / ON CONFLICT DO NOTHING.

**`erp.room_type`** — tipo de habitación por empresa

| Columna | Tipo | Nota |
|---|---|---|
| id, tenant_id, company_id | UUID | |
| name | VARCHAR(80) | Simple, Doble, Matrimonial, Suite... |
| capacity | INT | huéspedes que admite |
| rate_per_night | NUMERIC(10,2) | tarifa base por noche |
| description | VARCHAR(300) | opcional |
| is_active | BOOLEAN | |

**`erp.room`** — habitación física por sucursal

| Columna | Tipo | Nota |
|---|---|---|
| id, tenant_id, company_id, branch_id | UUID | |
| room_type_id | UUID FK erp.room_type | |
| number | VARCHAR(20) | único por (tenant, branch): "101", "2B" |
| floor | VARCHAR(20) | opcional |
| status | VARCHAR(20) | AVAILABLE, OCCUPIED, CLEANING, MAINTENANCE |
| notes | VARCHAR(300) | opcional |
| is_active | BOOLEAN | |

**`erp.stay`** — reserva/estadía (entidad central)

| Columna | Tipo | Nota |
|---|---|---|
| id, tenant_id, company_id, branch_id | UUID | |
| room_id | UUID FK erp.room | |
| customer_id | UUID FK erp.customer | huésped titular |
| check_in_date / check_out_date | DATE | salida > entrada; noches = diferencia |
| status | VARCHAR(20) | RESERVED, CHECKED_IN, CHECKED_OUT, CANCELED, NO_SHOW |
| rate_per_night | NUMERIC(10,2) | copiada del room_type, editable al reservar |
| guests_count | INT | |
| checked_in_at / checked_out_at | TIMESTAMPTZ | reales, nullables |
| sale_id | UUID nullable | venta generada en el check-out |
| ticket_code | VARCHAR(10) | código corto (reusar `TicketCodes`) |
| source | VARCHAR(20) | INTERNAL (fase 1); ONLINE reservado para fase 2 |
| notes | VARCHAR(500) | |

Índice de agenda: (`tenant_id`, `branch_id`, `check_in_date`, `check_out_date`).
**Regla de solape**: no pueden coexistir dos stays activas (RESERVED/CHECKED_IN) de la
misma habitación con rangos de fechas que se crucen — validado en el service dentro de la
transacción.

**`erp.stay_guest`** — registro de huéspedes (obligación legal peruana)

| Columna | Tipo | Nota |
|---|---|---|
| id, tenant_id, stay_id | UUID | |
| full_name | VARCHAR(120) | |
| doc_type / doc_number | VARCHAR(10) / VARCHAR(20) | DNI, CE, PASAPORTE |
| nationality | VARCHAR(60) | default Peru |

**`erp.stay_charge`** — cargos a la habitación durante la estadía

| Columna | Tipo | Nota |
|---|---|---|
| id, tenant_id, stay_id | UUID | |
| product_id | UUID nullable FK erp.product | si viene del inventario |
| description | VARCHAR(160) | libre si no hay producto |
| quantity | NUMERIC(10,2) / unit_price NUMERIC(10,2) | |
| created_by | UUID | trabajador que lo registró |

En el check-out, noches + cargos se convierten en una **venta del POS existente** (línea
"Hospedaje hab. 101 x N noches" + una línea por cargo); el cobro, arqueo, ticket y
facturación siguen el flujo actual sin cambios. `stay.sale_id` enlaza ambos mundos y los
cargos con `product_id` descuentan stock al venderse.

### 3.4 RBAC

Nuevos permisos en `auth.permission` (misma migración): **`lodging:read`** (ver rack,
reservas, huéspedes) y **`lodging:manage`** (crear/editar reservas, check-in/out, cargos,
habitaciones y tarifas). Asignar a los roles de tenant según el patrón de `appointment:*`
(admin y recepción con manage; consulta con read). Enforcement backend por tenant +
`requireCompany` + `BranchAccessService.assertCanOperate` en check-in/check-out y cargos
(el recepcionista solo opera su sede, patrón V48).

### 3.5 API (prefijo `/api/v1/erp`, paginado con `PageResponse<T>`)

| Método y ruta | Función |
|---|---|
| CRUD `/room-types`, `/rooms` | catálogo de tipos y habitaciones (`?companyId=`) |
| PATCH `/rooms/{id}/status` | cambiar estado (CLEANING → AVAILABLE, MAINTENANCE) |
| GET `/rooms/rack?branchId=&date=` | rack: habitaciones + estado + stay actual |
| GET `/stays?branchId=&status=&from=&to=` | lista paginada server-side |
| POST `/stays` | reservar (valida solape, capacidad, fechas) |
| POST `/stays/{id}/check-in` | pasa a CHECKED_IN, registra huéspedes, ocupa habitación |
| POST `/stays/{id}/charges` / GET | cargos de la estadía |
| POST `/stays/{id}/check-out` | genera venta POS, cierra estadía, habitación a CLEANING |
| POST `/stays/{id}/cancel` | cancela con motivo (ConfirmDialog en UI) |
| GET `/stays/availability?branchId=&from=&to=` | habitaciones libres en el rango |

Errores al cliente siempre con `ApiException` (gotcha del GlobalExceptionHandler).

### 3.6 SaaS (frontend)

- Registrar `lodging` en `module-registry.ts` → nav **"Hospedaje"** (icono cama hugeicons),
  ruta `/dashboard/lodging`, gateado por módulo + `lodging:read` (patrón saas-rbac-gating).
- Página con tabs (patrón POS/Inventario): **[Rack | Reservas | Habitaciones]**.
  - **Rack**: grilla de tarjetas por habitación con semáforo de estado (verde disponible,
    azul ocupada con nombre del huésped y fecha de salida, ámbar limpieza, gris
    mantenimiento); acciones directas check-in / check-out / marcar limpia; llegadas y
    salidas de hoy arriba.
  - **Reservas**: lista paginada con filtros (estado, rango, búsqueda por huésped o
    ticket), Nueva reserva (huésped: buscar cliente o crear al vuelo — misma pieza que el
    selector de cliente del POS pendiente), reprogramar y cancelar con ConfirmDialog.
  - **Habitaciones**: CRUD de tipos (tarifa, capacidad, InfoTip en campos) y habitaciones.
- Reglas de UI vigentes: paginación en todo (nada de scroll infinito), ConfirmDialog en
  acciones de un clic, sin emojis, texto real en español, diseño saas-design-system.

### 3.7 Onboarding

- El lookup por RUC sugiere el rubro: mapear CIIU **55xx** (551 hoteles, 552 otros tipos de
  alojamiento) → `lodging`.
- Pregunta de operación nueva en `OperationAnswers`: **`sellsFood`** ("¿Atiendes restaurante
  o cafetería dentro del hospedaje?") → activa `tables`, `kitchen`, `menu` (reutiliza el
  mapeo de `sellsOnTables`). El módulo `lodging` viene por defecto del rubro, no se pregunta.

### 3.8 Reportes (fase 1, mínimo)

En `/dashboard/reports`, visible solo con el módulo activo: **ocupación** (% habitaciones
ocupadas hoy y promedio 7/30 días), llegadas/salidas del día e ingreso por hospedaje vs
otros (las ventas de check-out ya entran a los KPIs de ventas existentes).

### 3.9 Entregables y criterios de aceptación

**Entregables**: (1) migración `V54__lodging_module.sql` completa e idempotente;
(2) módulo backend `com.sumaup360.erp.lodging` (web → service → domain → repository,
blueprint estándar); (3) UI SaaS descrita en 3.6; (4) actualización de
`docs/03-database-design.md`, `docs/04-saas-erp-modules.md` y `MODULE_SYSTEM.md`;
(5) E2E de humo en scratchpad (patrón e2e-caja.mjs).

**Criterios de aceptación (E2E)**:
1. Onboarding nuevo con rubro `lodging` → el dashboard muestra Hospedaje y los core; sin
   módulos de restaurante salvo respuesta afirmativa a `sellsFood`.
2. Crear tipo Matrimonial S/80 + habitaciones 101-103 → aparecen en el rack disponibles.
3. Reservar 101 para un cliente por 2 noches → intento de segunda reserva solapada de 101
   responde 4xx con mensaje claro; el rack la muestra ocupada tras el check-in.
4. Check-in registra 2 huéspedes con DNI (quedan en `stay_guest`).
5. Cargo de minibar con producto del inventario → descuenta stock al liquidarse.
6. Check-out genera venta cobrable en el POS (efectivo/Yape), emite ticket con
   serie/correlativo y deja la habitación en CLEANING; marcar limpia → AVAILABLE.
7. La ficha del cliente (`/customers/{id}/summary`) refleja la compra del check-out.
8. Un trabajador con sede asignada distinta recibe 403 al hacer check-in en otra sede.
9. Desactivar el módulo en Módulos oculta Hospedaje de la navegación sin romper nada.

### 3.10 Ampliación implementada (V55): alquiler por horas y walk-in

Práctica estándar del hospedaje peruano que la fase 1 no cubría:

- **Tarifa por hora** en `room_type.rate_per_hour` (null = ese tipo no se alquila por horas).
- **`stay.rental_mode`** (`NIGHTLY` | `HOURLY`) + **`stay.hours`**: una estadía por horas
  entra y sale el mismo día (`check_in_date = check_out_date`), no bloquea el solape
  nocturno y ocupa la habitación solo vía `room.status`. En HOURLY, `stay.rate_per_night`
  guarda la tarifa pactada por hora (la unidad la define `rental_mode`).
- **Walk-in**: `CreateStayRequest` acepta `rentalMode`, `hours`, `checkInNow` y `guests`;
  HOURLY siempre hace check-in inmediato (exige habitación AVAILABLE). NIGHTLY con
  `checkInNow` también ocupa al instante. Extender horas: `PUT /stays/{id}` con `hours`
  sobre una estadía HOURLY con check-in.
- **Check-out por horas**: liquida `horas × tarifa` + consumos como venta POS
  ("Hospedaje hab. 101 (3 horas)").
- **UI**: asistente de recepción en el Rack ("Alquilar habitación"): ¿por horas o por
  noche? → tarifario por tipo con habitaciones libres → habitación puntual → huésped
  (nuevo o existente, con DNI para el registro legal) → ocupar ya. El rack muestra
  "Por horas (N h) · sale ~HH:mm" y acceso directo a Consumos (cerveza, comida,
  lavandería... = `stay_charge` con producto del inventario o servicio libre).

### 3.11 Fase 2 (comprometida, no incluida)

Reserva online pública de habitaciones (patrón `booking_page` + QR + disponibilidad),
calendario mensual de ocupación, tarifas por temporada, exportable del registro de
huéspedes, housekeeping con asignación de personal, exoneración IGV no domiciliados
(junto con NubeFact).

---

*Referencias: `docs/04-saas-erp-modules.md`, `sumaup360-saas/docs/MODULE_SYSTEM.md`,
migraciones V2, V12, V38-V41 (patrón rubro veterinaria) y V47/V48 (aislamiento por
empresa y trabajadores).*
