-- Catalogo maestro de productos: el staff registra productos por rubro (con foto y
-- procedencia) y los tenants del SaaS los buscan para añadirlos a su inventario sin
-- volver a digitar ni subir fotos duplicadas. Una foto por producto, para todos.

create table catalog.master_product (
    id               uuid primary key,
    ean              varchar(20),
    name             varchar(160)  not null,
    brand            varchar(80),
    category         varchar(80),
    presentation     varchar(80),
    photo_url        varchar(600),
    photo_source     varchar(20),
    photo_source_url varchar(600),
    verified         boolean       not null default false,
    active           boolean       not null default true,
    created_at       timestamptz   not null,
    updated_at       timestamptz   not null
);

-- EAN unico cuando existe (productos a granel/sin codigo quedan null)
create unique index ux_master_product_ean on catalog.master_product (ean) where ean is not null;
create index ix_master_product_name on catalog.master_product (lower(name));

-- Rubros donde se ofrece el producto (una gaseosa no se muestra a una veterinaria)
create table catalog.master_product_rubro (
    product_id    uuid        not null references catalog.master_product (id) on delete cascade,
    business_type varchar(40) not null,
    primary key (product_id, business_type)
);
create index ix_master_product_rubro_bt on catalog.master_product_rubro (business_type);

-- El producto del tenant puede nacer del catalogo maestro (hereda foto por referencia)
alter table erp.product
    add column master_product_id uuid;

-- Permiso staff para administrar el catalogo maestro
insert into auth.permission (id, code, description, created_at, updated_at)
select gen_random_uuid(), 'catalog:product:manage',
       'Administrar el catalogo maestro de productos', now(), now()
on conflict (code) do nothing;

insert into auth.role_permission (role_id, permission_id)
select ro.id, pe.id
from auth.role ro
join auth.permission pe on pe.code = 'catalog:product:manage'
where ro.code in ('admin', 'soporte', 'logistica') and ro.tenant_id is null
on conflict do nothing;
