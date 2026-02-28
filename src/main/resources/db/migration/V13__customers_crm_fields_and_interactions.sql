-- CRM customer fields
alter table customers
    add column if not exists phone varchar(40) not null default '',
    add column if not exists email varchar(200) not null default '',
    add column if not exists address text not null default '',
    add column if not exists notes text;

-- Customer interactions (timeline / calendar)
create table if not exists customer_interactions (
    id bigserial primary key,
    customer_id bigint not null references customers(id) on delete cascade,
    interaction_date date not null,
    interaction_type varchar(30) not null,
    title varchar(200) not null,
    description text,
    created_by_username varchar(120),
    created_at timestamptz not null default now()
);

create index if not exists idx_customer_interactions_customer_date
    on customer_interactions(customer_id, interaction_date);

-- Customer files (metadata + filesystem storage)
create table if not exists customer_files (
    id bigserial primary key,
    customer_id bigint not null references customers(id) on delete cascade,
    file_name varchar(260) not null,
    content_type varchar(120),
    file_size bigint,
    storage_path varchar(500) not null,
    uploaded_by_username varchar(120),
    uploaded_at timestamptz not null default now()
);

create index if not exists idx_customer_files_customer
    on customer_files(customer_id);
