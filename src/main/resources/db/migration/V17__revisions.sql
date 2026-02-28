-- Revisions: enable revise/edit history for offers and orders

-- Offer revision metadata
alter table offers
  add column if not exists revision_no int not null default 0,
  add column if not exists last_revised_at timestamptz,
  add column if not exists last_revised_by_username varchar(120);

-- Order revision metadata
alter table orders
  add column if not exists revision_no int not null default 0,
  add column if not exists last_revised_at timestamptz,
  add column if not exists last_revised_by_username varchar(120);

-- Offer revisions table
create table if not exists offer_revisions (
  id bigserial primary key,
  offer_id bigint not null references offers(id) on delete cascade,
  revision_no int not null,
  revised_at timestamptz not null default now(),
  revised_by_username varchar(120),
  reason varchar(500),
  snapshot jsonb not null
);

create index if not exists idx_offer_revisions_offer_id on offer_revisions(offer_id);
create index if not exists idx_offer_revisions_offer_id_rev on offer_revisions(offer_id, revision_no);

-- Order revisions table
create table if not exists order_revisions (
  id bigserial primary key,
  order_id bigint not null references orders(id) on delete cascade,
  revision_no int not null,
  revised_at timestamptz not null default now(),
  revised_by_username varchar(120),
  reason varchar(500),
  snapshot jsonb not null
);

create index if not exists idx_order_revisions_order_id on order_revisions(order_id);
create index if not exists idx_order_revisions_order_id_rev on order_revisions(order_id, revision_no);
