SELECT U.*
FROM users U
INNER JOIN password_credentials P ON U.user_id = P.user_id
WHERE U.account = /*account*/'scott'
  AND P.password = HASH('SHA256', STRINGTOUTF8(CONCAT(P.salt, /*password*/'tiger')), 100)
