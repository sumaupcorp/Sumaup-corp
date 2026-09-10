-- Foto por URL externa (hotlink a la web del fabricante/tienda): se muestra primero y,
-- si la URL se cae, el front hace fallback a photo_url (la foto subida a nuestro Storage).
-- Sin ninguna de las dos, el producto se muestra con etiqueta "Sin foto".
alter table catalog.master_product
    add column photo_external_url varchar(600);
