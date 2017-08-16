SELECT RLM.realm_id, P.name AS permission
FROM permissions P
JOIN role_permissions RP ON RP.permission_id = P.permission_id
JOIN roles R ON R.role_id = RP.role_id
JOIN assignments A ON A.role_id = R.role_id
JOIN realms RLM ON RLM.realm_id = A.realm_id
JOIN groups G ON A.group_id = G.group_id
JOIN memberships M ON M.group_id = G.group_id
WHERE M.user_id = /*userId*/1
