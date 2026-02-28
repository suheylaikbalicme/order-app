-- Offer header extensions: payment term + conversion tracking

alter table offers
  add column if not exists payment_days int,
  add column if not exists converted_order_id bigint;
