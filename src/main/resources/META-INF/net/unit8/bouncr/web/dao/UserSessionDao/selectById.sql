SELECT US.*
FROM user_sessions US
WHERE US.user_id = /*principal.id*/1
  AND US.user_session_id = /*id*/1
