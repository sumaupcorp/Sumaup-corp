# app — Línea Personas (schema: app)

Dominio de personas naturales e independientes (taxistas, delivery, arrendadores,
profesionales con RUC, honorarios 4ta, Nuevo RUS). **No se mezcla** con la línea Negocios
(`erp`). Lo transversal (auth, billing, catalog...) se reutiliza, no se duplica.

Subpaquetes:

- `diagnosis` — diagnóstico tributario y recomendación de plan/segmento.
- `income` — registro de ingresos de la persona.
- `expense` — registro de gastos.
- `receipt` — recibos por honorarios y comprobantes.
- `alert` — alertas y recordatorios tributarios.
