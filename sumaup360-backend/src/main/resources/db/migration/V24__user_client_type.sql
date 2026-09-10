-- V24: tipo de cliente (TAXISTA / DELIVERY_PEYA) y DNI en el perfil de la persona.
-- Base del nuevo modelo de planes y modulos por tipo de cliente.

ALTER TABLE app.person_profile
    ADD COLUMN client_type VARCHAR(20),   -- TAXISTA | DELIVERY_PEYA
    ADD COLUMN dni         VARCHAR(15);    -- documento de identidad (para login/lookup SUNAT)
