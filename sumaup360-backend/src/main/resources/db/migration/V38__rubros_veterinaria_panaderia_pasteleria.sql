-- V38: rubros nuevos (veterinaria, panaderia, pasteleria) y modulos patients / custom-orders.
-- Los rubros se configuran, no se programan: solo catalogo + defaults de modulos.
-- Ademas: minimarket gana batch-expiry (perecibles) y se desactiva el vertical
-- restaurant/pasteleria para no duplicar "Pasteleria" con el rubro propio.

-- ---------------------------------------------------------------------------
-- Rubros nuevos
-- ---------------------------------------------------------------------------
INSERT INTO catalog.business_types (code, name) VALUES
    ('veterinary', 'Veterinaria'),
    ('bakery', 'Panaderia'),
    ('pastry', 'Pasteleria')
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Modulos nuevos (verticales, se habilitan por rubro)
-- ---------------------------------------------------------------------------
INSERT INTO catalog.modules (code, name, is_core) VALUES
    ('patients',      'Pacientes / Mascotas',  FALSE),
    ('custom-orders', 'Pedidos por encargo',   FALSE)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Defaults por rubro. El CROSS JOIN de core de V12 solo corrio para los rubros
-- que existian entonces: los rubros nuevos reciben el core aqui.
-- ---------------------------------------------------------------------------
INSERT INTO catalog.business_type_module (business_type_code, module_code)
SELECT bt.code, m.code
FROM catalog.business_types bt
CROSS JOIN catalog.modules m
WHERE m.is_core = TRUE
  AND bt.code IN ('veterinary', 'bakery', 'pastry')
ON CONFLICT DO NOTHING;

INSERT INTO catalog.business_type_module (business_type_code, module_code) VALUES
    ('veterinary', 'appointments'),
    ('veterinary', 'patients'),
    ('bakery',     'custom-orders'),
    ('bakery',     'batch-expiry'),
    ('pastry',     'custom-orders'),
    ('minimarket', 'batch-expiry')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Pasteleria ahora es rubro propio: se desactiva el vertical de restaurante
-- (no se borra: empresas existentes referencian el code).
-- ---------------------------------------------------------------------------
UPDATE catalog.verticals SET is_active = FALSE WHERE code = 'pasteleria';
