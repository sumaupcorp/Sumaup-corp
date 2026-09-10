# Sistema de módulos del dashboard

Este documento explica cómo **un solo dashboard se configura por rubro** mediante módulos.
No hay un dashboard por rubro: hay un ERP core que se adapta. Fuente de dominio:
`../../docs/04-saas-erp-modules.md` y la skill raíz `sumaup-domain-model`.

## Conceptos

| Concepto | Qué es | Quién manda |
|---|---|---|
| `businessType` | Rubro del negocio (restaurant, pharmacy, hardware, grocery, minimarket, bookstore, beauty, laundry). | Backend |
| `vertical` | Sub-rubro / especialización (ej. restaurant → cevicheria, polleria, chifa, pasteleria, cafeteria). | Backend |
| `enabledModules` | Conjunto efectivo de módulos activos para la empresa. **Construye el menú.** | Backend |
| `plan` | Paquete comercial: módulos permitidos + límites (sucursales, usuarios, transacciones). | Backend |
| `membership` | Vínculo empresa ↔ plan vigente (estado, fechas, ciclo). | Backend |
| `permissions` | Acciones `recurso:accion` que el usuario puede ejecutar (`sale:create`, etc.). | Backend |

## Cómo se resuelven los módulos (en el backend)

```
defaultModules(businessType, vertical)   // base por rubro/sub-rubro
   ∩  allowedModules(plan)               // techo por plan contratado
   ±  overrides(empresa)                 // feature flags/ajustes por empresa
   =  enabledModules(empresa)            // lo que la UI muestra
```

El **frontend NO calcula esto**. Recibe `enabledModules` ya resuelto y lo refleja.

## Cómo se construye el menú/navegación

1. El backend entrega `session = { tenant, company, businessType, vertical, plan, membership,
   enabledModules, permissions, roles }`.
2. El frontend consulta el **registro de módulos** (`src/lib/module-registry.ts`): para cada
   `ModuleKey` en `enabledModules` obtiene su metadata de UI (label, ruta, icono, grupo).
3. Se ordenan por `group` y se renderiza el sidebar. Un módulo ausente de `enabledModules`
   **no aparece** en el menú aunque exista su código.
4. Dentro de un módulo, las **acciones** se muestran/ocultan/deshabilitan según `permissions`
   (gating de UX, no de seguridad). Ver skill `saas-rbac-gating`.

```
enabledModules ──> moduleRegistry[key] ──> { label, path, group, icon } ──> Sidebar
permissions    ──> gating de botones/acciones dentro de cada vista
```

## Módulos del ERP core (comunes a todos los rubros)

`company`, `branch`, `customer`, `product`, `inventory`, `sale`, `pos`, `invoice`,
`supplier`, `report`.

## Extensiones por vertical (ejemplos)

| Rubro | Módulos extra |
|---|---|
| Restaurante | `tables`, `kitchen`, `menu` |
| Botica / Farmacia | `batch-expiry` (página `/dashboard/batches`), `prescription` (`/dashboard/prescriptions`) |
| Veterinaria | `appointments` (`/dashboard/appointments`, incluye reserva online por QR), `patients` (`/dashboard/patients`) |
| Panadería | `custom-orders` (`/dashboard/custom-orders`), `batch-expiry` |
| Pastelería | `custom-orders` |
| Minimarket | `batch-expiry` |
| Ferretería | `variants`, `bulk-units` |
| Bodega | core |
| Librería | `seasonal-catalog` |
| Belleza | `appointments`, `services` |
| Lavandería | `service-orders`, `tickets` |
| Hotel / Hospedaje | `lodging` (`/dashboard/lodging`: rack, reservas, check-in/out, habitaciones); `tables`/`kitchen`/`menu` solo si atiende restaurante (`sellsFood`) |

Nota: la `ModuleKey` del front debe ser **idéntica** al `code` de `catalog.modules` del
backend (el desalineado histórico `kitchen-orders` se corrigió a `kitchen`).

Las extensiones son **módulos + catálogos**, no aplicaciones separadas.

## Reglas

1. Nunca habilitar en el cliente un módulo que el backend no incluyó en `enabledModules`.
2. Añadir un rubro/vertical nuevo = registrar módulos + catálogos, **no** clonar el dashboard.
3. Antes de crear "algo para un rubro": ¿es módulo reutilizable, catálogo/config, o lógica
   única? (en ese orden de preferencia). Ver `../../docs/04-saas-erp-modules.md`.
