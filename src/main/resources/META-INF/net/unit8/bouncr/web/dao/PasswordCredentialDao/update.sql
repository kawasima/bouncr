UPDATE password_credentials
SET password = HASH('SHA256', STRINGTOUTF8(CONCAT(/*salt*/'0123456789012345', /*password*/'tiger')), 100)
  , salt = /*salt*/'0123456789012345'
WHERE user_id = /*id*/1
