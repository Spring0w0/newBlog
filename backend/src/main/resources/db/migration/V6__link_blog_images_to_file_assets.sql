ALTER TABLE blog_posts
    ADD COLUMN cover_file_asset_id BIGINT UNSIGNED NULL AFTER cover_url,
    ADD KEY idx_blog_posts_cover_file_asset_id (cover_file_asset_id),
    ADD CONSTRAINT fk_blog_posts_cover_file_asset
        FOREIGN KEY (cover_file_asset_id) REFERENCES file_assets (id);

ALTER TABLE blog_images
    ADD COLUMN file_asset_id BIGINT UNSIGNED NULL AFTER blog_id,
    ADD KEY idx_blog_images_file_asset_id (file_asset_id),
    ADD CONSTRAINT fk_blog_images_file_asset
        FOREIGN KEY (file_asset_id) REFERENCES file_assets (id);
