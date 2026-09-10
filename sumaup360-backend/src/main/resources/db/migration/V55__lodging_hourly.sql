-- V55: alquiler por horas (practica comun del hospedaje peruano) y walk-in de recepcion.
-- El tipo de habitacion gana una tarifa por hora opcional (null = ese tipo no se alquila
-- por horas). La estadia gana rental_mode (NIGHTLY | HOURLY) y hours: una estadia por
-- horas entra y sale el mismo dia (check_in_date = check_out_date), no reserva fechas
-- futuras (el solape nocturno la ignora) y ocupa la habitacion via room.status como
-- cualquier check-in. En estadias HOURLY, stay.rate_per_night guarda la tarifa pactada
-- POR HORA (misma columna, la unidad la define rental_mode).

ALTER TABLE erp.room_type ADD COLUMN IF NOT EXISTS rate_per_hour NUMERIC(10,2);

ALTER TABLE erp.stay ADD COLUMN IF NOT EXISTS rental_mode VARCHAR(10) NOT NULL DEFAULT 'NIGHTLY';
ALTER TABLE erp.stay ADD COLUMN IF NOT EXISTS hours INT;

-- La restriccion original exigia salida > entrada; por horas es el mismo dia.
ALTER TABLE erp.stay DROP CONSTRAINT IF EXISTS stay_check;
ALTER TABLE erp.stay ADD CONSTRAINT stay_dates_check CHECK (check_out_date >= check_in_date);
