UPDATE groups G
SET
  G.description = /*group.description*/'description',
  G.name = /*group.name*/'name'
WHERE G.group_id = /*group.id*/1
/*%if !principal.hasPermission("MODIFY_ANY_GROUP")*/
  AND EXISTS (
    SELECT * FROM users U
    JOIN memberships M ON U.user_id = M.user_id
    WHERE M.group_id = G.group_id
      AND U.account = /*principal.getName()*/'user1'
  )
/*%end*/
