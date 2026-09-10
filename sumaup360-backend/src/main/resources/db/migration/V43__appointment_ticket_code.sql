-- Codigo corto de ticket por cita: el cliente lo recibe al reservar (web publica) y el
-- negocio lo busca rapido en el panel. Unico por tenant; null en citas anteriores.
alter table erp.appointment
    add column ticket_code varchar(10);

create unique index ux_appointment_tenant_ticket
    on erp.appointment (tenant_id, ticket_code)
    where ticket_code is not null;
