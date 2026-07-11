ALTER TABLE bloggers
    ADD COLUMN avatar_file_asset_id BIGINT UNSIGNED NULL AFTER avatar_url,
    ADD KEY idx_bloggers_avatar_file_asset_id (avatar_file_asset_id),
    ADD CONSTRAINT fk_bloggers_avatar_file_asset
        FOREIGN KEY (avatar_file_asset_id) REFERENCES file_assets (id);

ALTER TABLE projects
    ADD COLUMN image_file_asset_id BIGINT UNSIGNED NULL AFTER image_url,
    ADD KEY idx_projects_image_file_asset_id (image_file_asset_id),
    ADD CONSTRAINT fk_projects_image_file_asset
        FOREIGN KEY (image_file_asset_id) REFERENCES file_assets (id);

ALTER TABLE shares
    ADD COLUMN logo_file_asset_id BIGINT UNSIGNED NULL AFTER logo_url,
    ADD KEY idx_shares_logo_file_asset_id (logo_file_asset_id),
    ADD CONSTRAINT fk_shares_logo_file_asset
        FOREIGN KEY (logo_file_asset_id) REFERENCES file_assets (id);

CREATE TABLE picture_images (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    picture_id VARCHAR(80) NOT NULL,
    file_asset_id BIGINT UNSIGNED NULL,
    url VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_picture_images_picture_id (picture_id),
    KEY idx_picture_images_file_asset_id (file_asset_id),
    CONSTRAINT fk_picture_images_picture_id
        FOREIGN KEY (picture_id) REFERENCES pictures (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_picture_images_file_asset
        FOREIGN KEY (file_asset_id) REFERENCES file_assets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE bloggers b
JOIN file_assets f
    ON f.scope = 'bloggers'
    AND (b.avatar_url = CONCAT('/images/', f.relative_path)
        OR b.avatar_url LIKE CONCAT('%/images/', f.relative_path))
SET b.avatar_file_asset_id = f.id
WHERE b.avatar_file_asset_id IS NULL;

UPDATE projects p
JOIN file_assets f
    ON f.scope = 'projects'
    AND (p.image_url = CONCAT('/images/', f.relative_path)
        OR p.image_url LIKE CONCAT('%/images/', f.relative_path))
SET p.image_file_asset_id = f.id
WHERE p.image_file_asset_id IS NULL;

UPDATE shares s
JOIN file_assets f
    ON f.scope = 'shares'
    AND (s.logo_url = CONCAT('/images/', f.relative_path)
        OR s.logo_url LIKE CONCAT('%/images/', f.relative_path))
SET s.logo_file_asset_id = f.id
WHERE s.logo_file_asset_id IS NULL;

INSERT INTO picture_images (picture_id, file_asset_id, url, sort_order)
SELECT
    p.id,
    f.id,
    image_item.url,
    image_item.sort_order - 1
FROM pictures p
JOIN JSON_TABLE(
    CASE WHEN JSON_VALID(p.images) THEN p.images ELSE JSON_ARRAY() END,
    '$[*]' COLUMNS (
        sort_order FOR ORDINALITY,
        url VARCHAR(1000) PATH '$'
    )
) image_item ON TRUE
LEFT JOIN file_assets f
    ON f.scope = 'pictures'
    AND (CONVERT(image_item.url USING utf8mb4) COLLATE utf8mb4_unicode_ci = CONCAT('/images/', f.relative_path)
        OR CONVERT(image_item.url USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE CONCAT('%/images/', f.relative_path));
