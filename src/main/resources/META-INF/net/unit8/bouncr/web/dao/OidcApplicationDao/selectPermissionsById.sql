SELECT *
FROM permissions P
JOIN oidc_application_scopes OAS ON OAS.permission_id = P.permission_id
JOIN oidc_applications OA ON OA.oidc_application_id = OAS.oidc_application_id
WHERE OA.oidc_application_id = /*id*/1

