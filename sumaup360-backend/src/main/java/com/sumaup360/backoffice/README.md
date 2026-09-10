# Módulo `backoffice` (interno SUMAUP)

Soporte de backend para la plataforma interna `sumaup360-backoffice`. Es **cross-tenant** y
solo para personal SUMAUP (staff). No mezclar con la lógica de clientes (`erp`) ni de la app
de personas (`app`).

Responsabilidades:
- **Staff:** identidades internas (marcadas como staff en el schema `auth`) y roles staff
  (`gerencia`, `admin`, `contador`, `desarrollador`, `soporte`, `logistica`).
- **Intake de comprobantes:** endpoints para que los contadores listen, tomen, procesen y
  cambien el estado de los comprobantes que suben los usuarios de la app
  (`pendiente → en_proceso → procesado | observado`). Operan sobre `app.receipt`.
- **Licenciamiento:** operaciones cross-tenant sobre `billing`/`subscription` (alta/baja/
  renovación de licencias del SaaS ERP por tenant).
- **Soporte, auditoría y reportes** internos.

Reglas: cada endpoint exige **rol staff + permiso**; el contexto staff es distinto del
contexto de tenant. Ver `../../../../../docs/10-backoffice-internal.md` y el agente
`backend-backoffice`.
