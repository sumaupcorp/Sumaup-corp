# 04 — Módulos del SaaS / ERP por rubro

Objetivo: un **solo ERP core** que sirve a todos los rubros mediante **módulos habilitados**
y configuración. **Prohibido** duplicar el sistema por rubro.

## Conceptos clave

- `businessType` (rubro): `restaurant`, `pharmacy`, `hardware`, `grocery`, `minimarket`,
  `bookstore`, `beauty`, `laundry`, `veterinary`, `bakery`, `pastry`, … (extensible).
- `vertical` (sub-rubro): especialización. Ej. `restaurant` → `cevicheria`, `polleria`,
  `chifa`, `cafeteria`. (`pasteleria` dejó de ser vertical de restaurante: ahora es el
  rubro propio `pastry`; el vertical quedó inactivo en el catálogo.)
- `module`: unidad funcional del ERP (`product`, `inventory`, `sale`, `pos`, `invoice`,
  `customer`, `supplier`, `report`, y extras de vertical como `tables`, `kitchen`).
- `plan`: paquete comercial que define módulos permitidos y límites.
- `membership`: vínculo empresa↔plan vigente.
- `enabledModules`: conjunto efectivo de módulos para una empresa.

## Cómo se resuelven los módulos de una empresa

```
defaultModules(businessType, vertical)      // base por rubro/sub-rubro
   ∩  allowedModules(plan)                   // techo por plan contratado
   ±  overrides(empresa)                     // ajustes/feature flags por empresa
   =  enabledModules(empresa)                // lo que la UI muestra y el backend permite
```

El backend es la fuente de verdad de `enabledModules`; el SaaS solo refleja y nunca habilita
algo que el backend no permita.

## Core ERP (común a todos los rubros)

`company`, `branch`, `customer`, `product`, `inventory`, `sale`, `pos`, `invoice`,
`supplier`, `report`.

## Extensiones por vertical (ejemplos)

| Rubro / sub-rubro | Módulos extra (defaults en `catalog.business_type_module`) | Estado |
|---|---|---|
| Restaurante (cevichería, pollería, chifa, cafetería) | `tables`, `kitchen`, `menu` | Implementado (V11) |
| Botica / Farmacia | `batch-expiry`, `prescription` | Implementado (V39: `erp.product_batch`, `erp.prescription`) |
| Veterinaria | `appointments`, `patients` | Implementado (V39: `erp.appointment`, `erp.patient`; reserva online V40) |
| Panadería | `custom-orders`, `batch-expiry` | Implementado (V39: `erp.custom_order`) |
| Pastelería | `custom-orders` | Implementado (V39) |
| Minimarket | `batch-expiry` (+ core) | Implementado |
| Ferretería | `variants`, `bulk-units` | Pendiente de dominio |
| Bodega | core | Implementado |
| Librería | `seasonal-catalog` | Pendiente de dominio |
| Belleza | `appointments`, `services` | Citas implementadas (V39); servicios pendiente |
| Lavandería | `service-orders`, `tickets` | Pendiente de dominio |
| Hotel / Hospedaje (hotel, hostal, apart-hotel, albergue, casa de huéspedes) | `lodging`; `tables`/`kitchen`/`menu` vía pregunta `sellsFood` | Implementado (V54: `erp.room_type`, `erp.room`, `erp.stay`, `erp.stay_guest`, `erp.stay_charge`; TDR en `11-rubro-hospedaje.md`) |

Las extensiones son **módulos y catálogos**, no aplicaciones separadas. Los módulos de V39
son genéricos y reutilizables entre rubros (`appointments` sirve a veterinaria y belleza;
`batch-expiry` a farmacia, panadería y minimarket).

## Reserva de citas online (módulo `appointments`, V40)

- `erp.booking_page`: configuración por empresa — token público del QR, `enabled`,
  `logo_url`, textos y `form_config` (JSON con los campos del **formulario dinámico** que el
  negocio edita desde su panel).
- Web pública en la landing (`sumaup360-reborn`): `/reservas/{token}` renderiza el
  formulario según `form_config` y crea la cita como `REQUESTED` (origen `ONLINE`) vía
  `/api/v1/public/booking/**` (sin auth, con rate-limit).
- El negocio confirma la cita desde el panel (Citas → Confirmar).

## Onboarding del SaaS (wizard)

1. **RUC opcional**: si el negocio lo tiene, `GET /api/v1/onboarding/ruc-lookup?ruc=`
   consulta la Ficha RUC (microservicio `sumaup360-sunat`), autocompleta razón social y
   sugiere el rubro por CIIU/actividad. Sin RUC se continúa igual.
2. Rubro + especialidad + nombre.
3. **Preguntas de operación** (`OnboardRequest.operation`): `sellsOnTables`, `tracksExpiry`,
   `takesAppointments`, `takesCustomOrders`, `handlesPrescriptions`. Semántica: `null` = no
   tocar los defaults del rubro; `true/false` = override del módulo tras el provision.
4. Sucursales.

## Regla de diseño

Antes de crear "algo para un rubro", clasifícalo:
1. ¿Es un **módulo** nuevo reutilizable? → añádelo al registro de módulos.
2. ¿Es **catálogo/configuración**? → va en `catalog` + config de la empresa.
3. ¿Es realmente lógica única? → justifícalo ante el master architect (excepción, no regla).

## UI del SaaS

- Dashboard único; el menú/navegación se construye desde `enabledModules`.
- No hay un dashboard por rubro: hay **un dashboard que se configura**.
- Tablas, KPIs (Recharts) y reportes son componentes genéricos parametrizados por módulo.

Ver el modelo de dominio completo en la skill `sumaup-domain-model`.
