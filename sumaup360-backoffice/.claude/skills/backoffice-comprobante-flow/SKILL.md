---
name: backoffice-comprobante-flow
description: Estados y reglas del procesamiento de comprobantes end-to-end (app móvil → backoffice → app). Úsalo al diseñar la cola de intake, transiciones de estado, asignación a contadores o cómo la app refleja el resultado.
---

# Skill — Flujo de comprobantes (end-to-end)

Ciclo de vida de un comprobante desde la app móvil (Línea Personas) hasta su procesamiento por
un contador (staff) en el backoffice, y su reflejo de vuelta en la app.

## Actores

- **Usuario de la app**: sube el comprobante (boleta/factura/RxH). NO es staff.
- **Contador**: staff que revisa, carga en SUMAUP360 y cambia el estado.
- **Backend**: autoridad; guarda el archivo, fija el estado inicial y valida transiciones.
- **SUMAUP360**: sistema contable donde el contador carga el comprobante.

## Estados

`pendiente · en_proceso · procesado · observado`.

Transiciones válidas:
```
pendiente  → en_proceso
en_proceso → procesado
en_proceso → observado
observado  → en_proceso   (tras corrección/reenvío del usuario)
```

## Flujo

1. Usuario sube comprobante en la app → backend lo crea como `pendiente`.
2. Aparece en la cola de intake del backoffice (`IntakeQueueItem`).
3. Un contador lo toma (`asignadoAContadorId`) → `en_proceso`.
4. Valida tipo, `montoSoles`, fecha y legibilidad; lo carga en SUMAUP360.
5. Conforme → `procesado`. Con problemas → `observado` + `nota` para el usuario.
6. El backend expone el estado; la app lo muestra. Si `observado`, el usuario corrige y reenvía.

## Reglas

- El frontend solo dispara cambios vía API; el backend valida transición y permisos.
- `observado` siempre lleva `nota`.
- Cada cambio de estado se registra en auditoría.
- Tipos: `src/types/comprobante.ts`. Detalle: `docs/COMPROBANTE_INTAKE.md`.
