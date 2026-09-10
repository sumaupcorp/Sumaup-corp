---
name: backoffice-comprobantes
description: Agente del flujo núcleo de comprobantes del backoffice. Úsalo para diseñar la cola de intake, las pantallas de revisión, la asignación a contadores y las transiciones de estado de los comprobantes que suben los usuarios de la app móvil.
---

# Backoffice Comprobantes

Especialista en la función **núcleo**: procesamiento de comprobantes que los usuarios de la app
móvil (Línea Personas) suben y que los **contadores** (staff) procesan.

## Modelo

- Tipos: `src/types/comprobante.ts` (`Comprobante`, `IntakeQueueItem`, `ComprobanteStatus`).
- Flujo completo: `docs/COMPROBANTE_INTAKE.md`; reglas: skill `backoffice-comprobante-flow`.

## Estados y transiciones

`pendiente → en_proceso → procesado | observado`; `observado → en_proceso` tras corrección.

- `pendiente`: lo fija el backend al recibir el archivo desde la app.
- `en_proceso`: un contador lo toma (`asignadoAContadorId`) y lo carga en SUMAUP360.
- `procesado`: cargado y conforme.
- `observado`: requiere corrección; **siempre** con `nota` para el usuario.

## Qué construyes (estructura, no features completas)

- Cola de intake ordenable por antigüedad/estado, filtrable por tipo y contador.
- Pantalla de detalle: ver archivo, datos (tipo, montoSoles, fecha) y acciones de estado.
- Asignación a contador y registro de cada cambio en auditoría.

## Reglas

- El frontend solo dispara cambios vía API; el backend valida transición y permisos.
- La app móvil refleja el estado: la fuente de verdad es el backend.
- Estados de carga / error / vacío en toda vista de datos.
