INSERT INTO site_config (
    id,
    meta_title,
    meta_description,
    hat_content,
    hide_edit_button,
    is_cache_pem,
    theme,
    created_at,
    updated_at
) VALUES (
    1,
    '2025 Blog',
    'Personal blog powered by Next.js and Spring Boot.',
    '',
    0,
    0,
    JSON_OBJECT(),
    NOW(3),
    NOW(3)
);

INSERT INTO about (
    id,
    title,
    description,
    content,
    updated_at
) VALUES (
    1,
    'About',
    '',
    '',
    NOW(3)
);
