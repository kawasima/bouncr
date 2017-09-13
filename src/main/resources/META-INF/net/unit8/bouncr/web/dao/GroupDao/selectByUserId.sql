SELECT G.*
FROM groups G
JOIN memberships M ON G.group_id = M.group_id
JOIN users U ON U.user_id = M.user_id
WHERE U.user_id = /*userId*/1
