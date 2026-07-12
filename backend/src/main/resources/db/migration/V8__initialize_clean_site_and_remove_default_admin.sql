-- 旧版本的 V2/V3 已进入 Flyway 历史，不能修改其校验和。
-- 本迁移仅将空站点补齐为通用配置，并移除旧的固定 admin/admin 初始化账号。

UPDATE site_config
SET
    meta_title = 'NewBlog',
    meta_description = '个人博客',
    theme = JSON_OBJECT(
        'meta', JSON_OBJECT('title', 'NewBlog', 'description', '个人博客'),
        'faviconUrl', '/favicon.png',
        'avatarUrl', '/images/avatar.png',
        'theme', JSON_OBJECT(
            'colorBrand', '#2fcbe7',
            'colorPrimary', '#5B423F',
            'colorSecondary', '#8b7667',
            'colorBrandSecondary', '#eec25e',
            'colorBg', '#d4e8f3',
            'colorBorder', '#ffffff',
            'colorCard', '#ffffff99',
            'colorArticle', '#ffffffcc'
        ),
        'backgroundColors', JSON_ARRAY('#f7da3987', '#8fdbe9', '#fffef8'),
        'artImages', JSON_ARRAY(),
        'currentArtImageId', '',
        'backgroundImages', JSON_ARRAY(),
        'currentBackgroundImageId', '',
        'socialButtons', JSON_ARRAY(),
        'clockShowSeconds', FALSE,
        'summaryInContent', FALSE,
        'isCachePem', FALSE,
        'hideEditButton', FALSE,
        'enableCategories', TRUE,
        'currentHatIndex', 1,
        'hatFlipped', FALSE,
        'enableChristmas', FALSE,
        'beian', JSON_OBJECT('text', '', 'link', '')
    )
WHERE id = 1
  AND (theme IS NULL OR JSON_LENGTH(theme) = 0);

UPDATE about
SET title = '关于', description = '', content = ''
WHERE id = 1
  AND title = 'About'
  AND COALESCE(description, '') = ''
  AND COALESCE(content, '') = '';

INSERT INTO card_styles (card_key, width, height, offset_x, offset_y, sort_order, enabled, config)
VALUES
    ('artCard', 360, 200, NULL, NULL, 3, TRUE, JSON_OBJECT('width', 360, 'height', 200, 'order', 3, 'offsetX', NULL, 'offsetY', NULL, 'enabled', TRUE)),
    ('hiCard', 360, 288, NULL, NULL, 1, TRUE, JSON_OBJECT('width', 360, 'height', 288, 'order', 1, 'offsetX', NULL, 'offsetY', NULL, 'enabled', TRUE)),
    ('clockCard', 232, 132, NULL, NULL, 4, TRUE, JSON_OBJECT('width', 232, 'height', 132, 'offset', 92, 'order', 4, 'offsetX', NULL, 'offsetY', NULL, 'enabled', TRUE)),
    ('calendarCard', 350, 286, NULL, NULL, 5, TRUE, JSON_OBJECT('width', 350, 'height', 286, 'order', 5, 'offsetX', NULL, 'offsetY', NULL, 'enabled', TRUE)),
    ('musicCard', 293, 66, NULL, NULL, 6, TRUE, JSON_OBJECT('width', 293, 'height', 66, 'offset', 120, 'order', 6, 'offsetX', NULL, 'offsetY', NULL, 'enabled', TRUE)),
    ('socialButtons', 315, 48, NULL, NULL, 6, TRUE, JSON_OBJECT('width', 315, 'height', 48, 'order', 6, 'offsetX', NULL, 'offsetY', NULL, 'enabled', TRUE)),
    ('shareCard', 200, 180, NULL, NULL, 7, TRUE, JSON_OBJECT('width', 200, 'height', 180, 'order', 7, 'offsetX', NULL, 'offsetY', NULL, 'enabled', TRUE)),
    ('articleCard', 266, 160, NULL, NULL, 8, TRUE, JSON_OBJECT('width', 266, 'height', 160, 'order', 8, 'offsetX', NULL, 'offsetY', NULL, 'enabled', TRUE)),
    ('writeButtons', 180, 42, NULL, NULL, 8, TRUE, JSON_OBJECT('width', 180, 'height', 42, 'order', 8, 'offsetX', NULL, 'offsetY', NULL, 'enabled', TRUE)),
    ('navCard', 280, 434, NULL, NULL, 2, TRUE, JSON_OBJECT('width', 280, 'height', 434, 'order', 2, 'offsetX', NULL, 'offsetY', NULL, 'enabled', TRUE)),
    ('likePosition', 54, 54, NULL, NULL, 8, TRUE, JSON_OBJECT('width', 54, 'height', 54, 'order', 8, 'offsetX', NULL, 'offsetY', NULL, 'enabled', TRUE)),
    ('hatCard', 99, 105, -48, -168, 10, FALSE, JSON_OBJECT('width', 99, 'height', 105, 'order', 10, 'offsetX', -48, 'offsetY', -168, 'enabled', FALSE)),
    ('beianCard', 200, 60, NULL, NULL, 11, FALSE, JSON_OBJECT('width', 200, 'height', 60, 'order', 11, 'offsetX', NULL, 'offsetY', NULL, 'enabled', FALSE))
ON DUPLICATE KEY UPDATE card_key = card_key;

DELETE FROM users
WHERE username = 'admin'
  AND password_hash = '$2b$12$Y4hbLBZc69SW.ne5vIVkDethbJpa78kG0X2fuX/Vtz0x5bVjiQWUy'
  AND role = 'ADMIN'
  AND enabled = TRUE;
