# erp — Línea Negocios (schema: erp)

Dominio de administración de negocios: empresas con sucursales, trabajadores, inventario,
ventas y caja/POS. **No se mezcla** con la Línea Personas (`app`).

Rubros por configuración: el ERP core es uno solo; los rubros (restaurante, botica,
ferretería, etc.) y sus verticales (cevichería, pollería...) se habilitan vía catálogos
(`catalog`) y módulos habilitados por rubro/plan, **sin duplicar código**.

Subpaquetes:

- `company` — empresa / tenant de negocio.
- `branch` — sucursales.
- `businesstype` — rubro y verticales aplicados a la empresa.
- `module` — módulos habilitados por rubro/plan.
- `customer` — clientes.
- `product` — productos y catálogo propio.
- `inventory` — stock y movimientos.
- `sale` — ventas.
- `pos` — caja / punto de venta.
- `invoice` — comprobantes (factura/boleta).
- `supplier` — proveedores.
- `report` — reportes del negocio.
