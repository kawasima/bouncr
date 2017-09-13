SELECT DISTINCT G.*
FROM groups G
/*%if !principal.hasPermission("LIST_ANY_GROUPS")*/
JOIN memberships M ON G.group_id = M.group_id
JOIN users U ON M.user_id = U.user_id
/*%end*/
WHERE
/*%if !principal.hasPermission("LIST_ANY_GROUPS")*/
  U.account = /*principal.getName()*/'user1'
/*%end*/
ORDER BY G.group_id
