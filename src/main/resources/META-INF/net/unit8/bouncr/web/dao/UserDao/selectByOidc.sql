SELECT U.*
FROM users U
JOIN oidc_users OU ON U.user_id = OU.user_id
WHERE OU.oidc_provider_id = /*oidcProviderId*/1
  AND OU.oidc_sub = /*sub*/'abc'
