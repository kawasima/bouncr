SELECT DISTINCT P.*
FROM permissions P
/*%if !principal.hasPermission("LIST_ANY_PERMISSIONS") */
JOIN role_permissions RP ON RP.permission_id = P.permission_id
JOIN roles R ON R.role_id = RP.role_id
JOIN assignments A ON A.role_id = R.role_id
JOIN memberships M ON A.group_id = M.group_id
JOIN users U ON U.user_id = M.user_id
/*%end*/
WHERE /*%if !principal.hasPermission("LIST_ANY_PERMISSIONS") */
U.account = /*principal.getName()*/'user1'
/*%end*/
ORDER BY P.permission_id;
