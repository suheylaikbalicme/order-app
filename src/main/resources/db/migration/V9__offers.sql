-- OFFERS (teklifler)
create table if not exists offers (
  id bigserial primary key,
  customer_code varchar(50) not null,
  customer_name varchar(200),
  validity_days int,
  status varchar(30) not null default 'DRAFT',
  created_by bigint references users(id),
  created_by_username varchar(120),
  created_at timestamptz not null default now()
);

-- OFFER_ITEMS (satır bazlı KDV dahil)
create table if not exists offer_items (
  id bigserial primary key,
  offer_id bigint not null references offers(id) on delete cascade,
  item_code varchar(50) not null,
  item_name varchar(200),
  quantity numeric(12,3) not null,
  unit_price numeric(12,2) default 0,
  discount_rate numeric(6,2) default 0,
  vat_rate numeric(6,2) default 20
);
