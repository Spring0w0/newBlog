-- Keep static fallback assets outside /images so production Nginx can reserve /images for uploaded files.
UPDATE site_config
SET theme = JSON_SET(
        COALESCE(theme, JSON_OBJECT()),
        '$.avatarUrl',
        CASE
            WHEN JSON_UNQUOTE(JSON_EXTRACT(COALESCE(theme, JSON_OBJECT()), '$.avatarUrl')) = '/images/avatar.png'
                THEN '/avatar.png'
            ELSE COALESCE(JSON_UNQUOTE(JSON_EXTRACT(COALESCE(theme, JSON_OBJECT()), '$.avatarUrl')), '/avatar.png')
        END,
        '$.hiCard',
        COALESCE(
                JSON_EXTRACT(theme, '$.hiCard'),
                JSON_OBJECT(
                        'greeting', '',
                        'introPrefix', 'I''m',
                        'introSuffix', 'Nice to meet you!',
                        'avatarLink', '/live2d'
                )
        )
    )
WHERE id = 1;
