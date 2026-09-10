# features/comprobantes — Procesamiento de comprobantes (NÚCLEO)

Cola de comprobantes que los usuarios de la app móvil (Línea Personas) suben. Los contadores
los revisan, los asignan, los cargan en SUMAUP360 y cambian el estado
(`pendiente → en_proceso → procesado | observado`). La app móvil refleja el estado.

Tipos: `../../types/comprobante.ts`. Flujo: `../../../docs/COMPROBANTE_INTAKE.md` y skill
`backoffice-comprobante-flow`. El backend es la autoridad del estado.
