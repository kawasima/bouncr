SELECT DISTINCT R.*
FROM realms R
JOIN applications APP ON APP.application_id = R.application_id
/*%if !principal.hasPermission("LIST_ANY_REALMS") */
JOIN assignments A ON R.realm_id = A.realm_id
JOIN groups G ON G.group_id = A.group_id
JOIN members M ON M.group_id = G.group_id
JOIN users U ON U.user_id = M.user_id
/*%end*/
WHERE
/*%if !principal.hasPermission("LIST_ANY_REALMS") */
  U.account = /*principal.getName()*/'user1'
/*%end*/
  AND APP.application_id = /*applicationId*/1
ORDER BY R.realm_id

