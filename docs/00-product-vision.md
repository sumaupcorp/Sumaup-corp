# 00 — Visión de producto

## Qué es SUMAUP360

Ecosistema **fintech / legaltech peruano** que ordena la vida tributaria y operativa de dos
públicos distintos, bajo una misma marca y una misma mascota (Suma, mono azul).

## Dos líneas de producto

### Línea Personas — App móvil (Flutter)
Para personas naturales e independientes. Segmentos: taxistas, repartidores de delivery
(PedidosYa, Rappi, etc.), arrendadores de propiedades, profesionales con RUC, recibos por
honorarios (4ta categoría), pequeños contribuyentes / Nuevo RUS.

Promesa: "ordena tus ingresos, gastos e impuestos sin ser contador". Funciones: onboarding,
login, diagnóstico tributario inicial, ingresos, gastos, recibos, alertas, recomendación de
plan, perfil, chatbot Suma, suscripción.

### Línea Negocios — SaaS / ERP web (Next.js)
Para negocios peruanos con sucursales y trabajadores: boticas, farmacias, ferreterías,
restaurantes (cevichería, pollería, chifa, pastelería, cafetería), bodegas, minimarkets,
librerías, belleza, lavanderías y rubros futuros.

Promesa: "administra tu negocio completo (inventario, ventas, caja, reportes) según tu
rubro". Multi-tenant: cada empresa con sus sucursales, usuarios, roles y módulos habilitados.

### Línea Interna — Backoffice SUMAUP (Next.js)
Plataforma interna para el personal de SUMAUP (gerencia, admin, contadores, desarrollo,
soporte, logística). Cross-tenant y cima del RBAC. Funciones: **procesar los comprobantes**
que suben los usuarios de la app (los contadores los cargan en SUMAUP360), dar **soporte** a
esos usuarios y **gestionar las licencias/membresías** del SaaS ERP. Ver
`10-backoffice-internal.md`.

### Landing web (Next.js — ya existe)
Sitio comercial que presenta las líneas de cliente (Personas y Negocios), captura leads,
ofrece diagnóstico inicial, chatbot Suma y formularios de demo / lista de espera.

## Objetivo de negocio

- Convertir informalidad/desorden en orden financiero y tributario.
- Monetizar por **planes y membresías** por línea, con addons (ej. AI-CRM en el SaaS).
- Escalar por rubros sin reescribir el producto (configuración, no código duplicado).

## Principios de producto

1. Personas y Negocios son experiencias separadas; solo comparten lo transversal.
2. Un solo ERP core; los rubros son configuración.
3. El diagnóstico recomienda plan: es la puerta de entrada en ambas líneas.
4. Suma (mascota + chatbot) acompaña toda la experiencia.

## Métricas que importan (norte)

- Leads → diagnósticos completados → demos/suscripciones.
- Activación (primer ingreso/venta registrada) y retención por línea.
- Conversión free→pago y upgrades de plan.
