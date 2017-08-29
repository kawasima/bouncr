SELECT *
FROM groups
WHERE name like /*@prefix(word)*/'a' escape '$'
   OR description like /*@infix(word)*/'a' escape '$'
