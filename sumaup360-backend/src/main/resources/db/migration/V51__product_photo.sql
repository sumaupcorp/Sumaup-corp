-- V51: foto propia del producto del negocio. Complementa a la heredada del catalogo
-- maestro: si el producto tiene foto propia, esa manda; si no, se hereda la del maestro.
-- La imagen vive en Firebase Storage (products/{tenantId}/...), aqui solo la URL.

ALTER TABLE erp.product ADD COLUMN photo_url VARCHAR(500);
