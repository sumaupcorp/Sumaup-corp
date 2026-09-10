-- V50: foto del paciente/mascota (modulo patients). La imagen vive en Firebase
-- Storage; aqui solo se guarda la URL publica.

ALTER TABLE erp.patient ADD COLUMN photo_url VARCHAR(500);
