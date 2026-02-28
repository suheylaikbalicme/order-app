create table if not exists customers (
  id bigserial primary key,
  customer_code varchar(50) not null,
  customer_name varchar(200) not null,
  sync_status varchar(20) not null default 'PENDING',
  logo_ref varchar(120),
  sync_error text,
  created_by bigint references users(id),
  created_at timestamptz not null default now()
);

create index if not exists idx_customers_created_by on customers(created_by);
create index if not exists idx_customers_code on customers(customer_code);