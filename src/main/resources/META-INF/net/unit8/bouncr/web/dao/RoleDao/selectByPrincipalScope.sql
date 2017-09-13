SELECT R.*
FROM roles R
/*%if !principal.hasPermission("LIST_ANY_ROLES") */
JOIN assignments A ON R.role_id = A.role_id
JOIN groups G ON G.group_id = A.group_id
JOIN memberships M ON M.group_id = G.group_id
JOIN users U ON U.user_id = M.user_id
/*%end*/
WHERE
/*%if !principal.hasPermission("LIST_ANY_ROLES") */
  U.account = /*principal.getName()*/'user1'
/*%end*/
ORDER BY R.role_id
