-- V52: ficha completa del proveedor: persona de contacto, direccion, notas y
-- estado activo (los proveedores con los que ya no se trabaja se desactivan,
-- no se borran: el historial se conserva).

ALTER TABLE erp.supplier ADD COLUMN contact_name VARCHAR(120);
ALTER TABLE erp.supplier ADD COLUMN address      VARCHAR(255);
ALTER TABLE erp.supplier ADD COLUMN notes        VARCHAR(500);
ALTER TABLE erp.supplier ADD COLUMN is_active    BOOLEAN NOT NULL DEFAULT TRUE;
