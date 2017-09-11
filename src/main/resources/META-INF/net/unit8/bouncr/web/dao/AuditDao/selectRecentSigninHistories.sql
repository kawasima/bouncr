SELECT A.name
FROM user_actions UA
JOIN actions A ON A.action_id = UA.action_id
WHERE UA.actor = /*account*/'user'
  AND A.name in ('user.signin', 'user.failed_signin')
ORDER BY UA.created_at DESC