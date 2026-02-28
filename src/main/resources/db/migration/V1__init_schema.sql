-- ROLES
create table if not exists roles (
  id bigserial primary key,
  name varchar(50) not null unique
);

-- USERS
create table if not exists users (
  id bigserial primary key,
  username varchar(120) not null unique,
  password_hash varchar(200) not null,
  enabled boolean not null default true,
  created_at timestamptz not null default now()
);

-- USER_ROLES
create table if not exists user_roles (
  user_id bigint not null references users(id) on delete cascade,
  role_id bigint not null references roles(id) on delete cascade,
  primary key (user_id, role_id)
);

-- ORDERS (yerel sipariş kaydı)
create table if not exists orders (
  id bigserial primary key,
  customer_code varchar(50) not null,
  customer_name varchar(200),
  status varchar(30) not null default 'DRAFT',
  discount_rate numeric(6,2) default 0,
  vat_rate numeric(6,2) default 20,
  created_by bigint references users(id),
  created_at timestamptz not null default now()
);

-- ORDER_ITEMS
create table if not exists order_items (
  id bigserial primary key,
  order_id bigint not null references orders(id) on delete cascade,
  item_code varchar(50) not null,
  item_name varchar(200),
  quantity numeric(12,3) not null,
  unit_price numeric(12,2) default 0,
  discount_rate numeric(6,2) default 0,
  vat_rate numeric(6,2) default 20
);

-- Seed roles
insert into roles(name) values ('ADMIN') on conflict do nothing;
insert into roles(name) values ('USER') on conflict do nothing;
insert into roles(name) values ('VIEWER') on conflict do nothing;
