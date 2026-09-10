-- Estilo visual del QR de la pagina de reserva (preset + colores + logo al centro).
-- JSON como texto, igual que form_config; null = estilo clasico por defecto.
alter table erp.booking_page
    add column qr_style text;
