SELECT UPF.name, UPF.json_name, UPV.value
FROM user_profile_fields UPF
LEFT OUTER JOIN user_profile_values UPV ON UPF.user_profile_field_id=UPV.user_profile_field_id
JOIN users U ON U.user_id = UPV.user_id
WHERE U.user_id = /*userId*/1
