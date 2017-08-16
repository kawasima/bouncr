INSERT INTO password_credentials(user_id, password, salt)
VALUES(
    /*id*/1,
    HASH('SHA256', STRINGTOUTF8(CONCAT(/*salt*/'0123456789012345', /*password*/'tiger')), 100)
    /*salt*/'0123456789012345'
)
