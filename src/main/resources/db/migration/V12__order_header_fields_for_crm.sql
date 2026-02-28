-- CRM alignment: order header fields (currency, exchange rate, note, totals)

alter table orders
  add column if not exists order_date date,
  add column if not exists currency varchar(10),
  add column if not exists exchange_rate numeric(12,6),
  add column if not exists note varchar(2000),
  add column if not exists subtotal_amount numeric(14,2) default 0,
  add column if not exists discount_total numeric(14,2) default 0,
  add column if not exists vat_total numeric(14,2) default 0,
  add column if not exists grand_total numeric(14,2) default 0;
