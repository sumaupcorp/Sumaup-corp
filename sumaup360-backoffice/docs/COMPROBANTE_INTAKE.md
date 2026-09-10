# COMPROBANTE_INTAKE — Flujo de intake y procesamiento de comprobantes

Función **núcleo** del backoffice. Describe el ciclo de vida de un comprobante desde que un
usuario de la app móvil lo sube hasta que el contador lo procesa y la app refleja el resultado.

> Actores: **usuario de la app** (Línea Personas, sube el comprobante) · **contador** (staff,
> lo procesa) · **backend** (autoridad, guarda y valida) · **SUMAUP360** (sistema contable
> donde se carga). No confundir con trabajadores de un tenant.

## Estados

| Estado        | Significado                                                            | Quién lo fija          |
|---------------|------------------------------------------------------------------------|------------------------|
| `pendiente`   | Recién subido por el usuario; en cola, sin tomar.                      | Backend (al recibir)   |
| `en_proceso`  | Un contador lo tomó/está cargándolo en SUMAUP360.                      | Contador (backoffice)  |
| `procesado`   | Cargado y conforme. Cierra el ciclo.                                   | Contador (backoffice)  |
| `observado`   | Falta info / ilegible / incorrecto. Requiere acción del usuario.      | Contador (backoffice)  |

Transiciones válidas:

```
pendiente  → en_proceso
en_proceso → procesado
en_proceso → observado
observado  → en_proceso   (tras corrección/reenvío del usuario)
```

## Flujo end-to-end

1. **Captura (app móvil)**: el usuario fotografía/sube una boleta, factura o recibo por
   honorarios (RxH). Indica tipo y, si aplica, monto.
2. **Recepción (backend)**: guarda el archivo y los metadatos; crea el comprobante en
   `pendiente`. Es la autoridad del estado.
3. **Cola (backoffice)**: el comprobante aparece en la **cola de intake** (`IntakeQueueItem`).
   Visible para contadores (y lectura para soporte/gerencia según RBAC).
4. **Asignación**: un contador lo toma (`asignadoAContadorId`) y pasa a `en_proceso`.
5. **Revisión y carga**: el contador valida datos (tipo, monto en soles, fecha, legibilidad) y
   lo **carga en SUMAUP360**.
6. **Cierre**:
   - Todo correcto → `procesado`.
   - Algo falta o está mal → `observado` con `nota` explicativa para el usuario.
7. **Reflejo en la app**: el backend expone el estado; la app móvil lo muestra al usuario. Si
   está `observado`, el usuario corrige y reenvía, y el ciclo vuelve a `en_proceso`.

## Reglas

- El frontend **solo** dispara cambios vía API; el backend valida transición y permisos.
- Cada cambio de estado debería quedar en **auditoría** (módulo `audit`).
- Un comprobante `observado` siempre lleva `nota` para que el usuario sepa qué corregir.
- Tipos en `src/types/comprobante.ts`; reglas detalladas en skill `backoffice-comprobante-flow`.
