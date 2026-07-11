INSERT INTO users (username, password_hash, role, enabled)
VALUES ('admin', '$2b$12$Y4hbLBZc69SW.ne5vIVkDethbJpa78kG0X2fuX/Vtz0x5bVjiQWUy', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    enabled = VALUES(enabled);
