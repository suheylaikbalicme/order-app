-- Create admin user if not exists
insert into users(username, password_hash, enabled)
select 'admin', 'BURAYA_BCRYPT_HASH', true
where not exists (select 1 from users where username = 'admin');

-- Attach ADMIN role
insert into user_roles(user_id, role_id)
select u.id, r.id
from users u
join roles r on r.name = 'ADMIN'
where u.username = 'admin'
  and not exists (
      select 1 from user_roles ur
      where ur.user_id = u.id and ur.role_id = r.id
  );
