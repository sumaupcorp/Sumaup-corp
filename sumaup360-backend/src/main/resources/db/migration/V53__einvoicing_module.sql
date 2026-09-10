-- V53: modulo de FACTURACION ELECTRONICA, apagable por negocio.
-- Muchos negocios solo llevan control interno (tickets/notas de venta) y no emiten
-- boletas ni facturas: para ellos el modulo queda desactivado y el SaaS oculta todo
-- lo fiscal (tipos de documento tributarios, QR SUNAT, series oficiales).
-- No se asigna a ningun rubro por defecto: cada negocio lo ACTIVA cuando lo necesita
-- desde Modulos. Los planes "todos los modulos" lo incluyen en su techo.

INSERT INTO catalog.modules (code, name, is_core) VALUES
    ('e-invoicing', 'Facturacion electronica', FALSE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO billing.plan_module (plan_id, module_code)
SELECT p.id, 'e-invoicing'
FROM billing.plan p
WHERE p.code IN ('erp-free', 'erp-full')
ON CONFLICT DO NOTHING;
