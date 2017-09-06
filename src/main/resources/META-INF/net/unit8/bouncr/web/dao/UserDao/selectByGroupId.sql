SELECT U.*
FROM users U
JOIN memberships M ON M.user_id = U.user_id
WHERE M.group_id = /*groupId*/1
