-- Add offer header fields for real "Yeni Teklif" form

alter table offers
  add column if not exists offer_date date,
  add column if not exists currency varchar(10),
  add column if not exists exchange_rate numeric(12,6),
  add column if not exists note varchar(2000),
  add column if not exists subtotal_amount numeric(14,2) default 0,
  add column if not exists discount_total numeric(14,2) default 0,
  add column if not exists vat_total numeric(14,2) default 0,
  add column if not exists grand_total numeric(14,2) default 0;
