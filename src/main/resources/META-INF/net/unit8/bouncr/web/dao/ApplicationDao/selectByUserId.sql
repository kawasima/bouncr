SELECT DISTINCT APP.*
FROM applications APP
JOIN realms RLM ON RLM.application_id = APP.application_id
JOIN assignments A ON A.realm_id = RLM.realm_id
JOIN groups G ON G.group_id = A.group_id
JOIN memberships M ON M.group_id = G.group_id
WHERE M.user_id = /*userId*/1
