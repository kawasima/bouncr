SELECT P.*
FROM permissions P
JOIN role_permissions RP ON RP.permission_id = P.permission_id
WHERE role_id = /*roleId*/1
