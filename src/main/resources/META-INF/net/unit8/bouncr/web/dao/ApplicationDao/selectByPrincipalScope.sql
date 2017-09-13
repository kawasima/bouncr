SELECT DISTINCT APP.*
FROM applications APP
/*%if !principal.hasPermission("LIST_ANY_APPLICATIONS") */
JOIN realms R ON R.application_id = APP.application_id
JOIN assignments A ON R.realm_id = A.realm_id
JOIN groups G ON A.group_id = G.group_id
JOIN memberships M ON G.group_id = M.group_id
JOIN users U ON M.user_id = U.user_id
/*%end*/
WHERE
/*%if !principal.hasPermission("LIST_ANY_APPLICATIONS") */
  U.account = /*principal.getName()*/'user1'
/*%end*/
ORDER BY APP.application_id
