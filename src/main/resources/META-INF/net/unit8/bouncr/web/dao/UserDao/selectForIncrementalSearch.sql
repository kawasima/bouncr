SELECT *
FROM users U
/*%if !principal.hasPermission("LIST_ANY_USERS") */
JOIN memberships M ON M.user_id = U.user_id
JOIN (SELECT IG.*
  FROM groups IG
  JOIN memberships IM ON IG.group_id = IM.group_id
  JOIN users IU ON IM.user_id = IU.user_id
  WHERE IU.account = /*principal.getName()*/'user1'
) G ON G.group_id = M.group_id
/*%end*/
WHERE account like /*@prefix(word)*/'a' escape '$'
   OR email like /*@prefix(word)*/'a' escape '$'
   OR name like /*@prefix(word)*/'a' escape '$'
ORDER BY U.user_id
