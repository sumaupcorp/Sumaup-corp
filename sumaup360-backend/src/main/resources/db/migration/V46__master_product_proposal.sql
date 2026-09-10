-- Propuestas de producto al catalogo maestro (crowdsourcing con curacion):
-- cuando un tenant crea un producto propio que no esta en el catalogo, se registra una
-- propuesta PENDIENTE. El staff la aprueba (crea el master_product y vincula el producto
-- del tenant) o la rechaza. Los tenants alimentan el catalogo sin trabajo del staff mas
-- alla de un clic por propuesta.
create table catalog.master_product_proposal (
    id            uuid primary key,
    tenant_id     uuid          not null,
    company_id    uuid,
    product_id    uuid,
    business_type varchar(40),
    ean           varchar(20),
    name          varchar(160)  not null,
    category      varchar(80),
    status        varchar(15)   not null default 'PENDIENTE',
    created_at    timestamptz   not null,
    updated_at    timestamptz   not null
);

create index ix_master_product_proposal_status on catalog.master_product_proposal (status);
