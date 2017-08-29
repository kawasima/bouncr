SELECT *
FROM users
WHERE account like /*@prefix(word)*/'a' escape '$'
   OR email like /*@prefix(word)*/'a' escape '$'
   OR name like /*@prefix(word)*/'a' escape '$'
