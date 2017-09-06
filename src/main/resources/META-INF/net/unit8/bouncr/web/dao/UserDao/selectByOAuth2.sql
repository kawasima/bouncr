SELECT U.*
FROM users U
JOIN oauth2_users OU ON U.user_id = OU.user_id
WHERE OU.oauth2_provider_id = /*oauth2ProviderId*/1
  AND OU.oauth2_account = /*oauth2Account*/'abc'
