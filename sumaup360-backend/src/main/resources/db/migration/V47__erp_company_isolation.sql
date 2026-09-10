-- Aislamiento por EMPRESA dentro del tenant: productos, clientes y proveedores pasan a
-- pertenecer a una empresa (company), no a la cuenta completa. Un tenant con veterinaria y
-- farmacia deja de compartir inventario/clientes entre ambas. El stock y las ventas ya
-- estaban aislados por sucursal; esto completa el nivel que faltaba.

alter table erp.product add column company_id uuid;
alter table erp.customer add column company_id uuid;
alter table erp.supplier add column company_id uuid;

-- Backfill: los datos existentes se asignan a la primera empresa del tenant.
update erp.product p
set company_id = (select c.id from tenant.company c
                  where c.tenant_id = p.tenant_id order by c.created_at asc limit 1)
where p.company_id is null;

update erp.customer cu
set company_id = (select c.id from tenant.company c
                  where c.tenant_id = cu.tenant_id order by c.created_at asc limit 1)
where cu.company_id is null;

update erp.supplier su
set company_id = (select c.id from tenant.company c
                  where c.tenant_id = su.tenant_id order by c.created_at asc limit 1)
where su.company_id is null;

-- SKU unico por empresa (antes por tenant): dos negocios del mismo dueño pueden repetir SKU.
alter table erp.product drop constraint if exists product_tenant_id_sku_key;
create unique index ux_product_company_sku on erp.product (tenant_id, company_id, sku);

create index ix_product_company on erp.product (company_id);
create index ix_customer_company on erp.customer (company_id);
create index ix_supplier_company on erp.supplier (company_id);
