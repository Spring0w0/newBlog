CREATE TABLE file_assets (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    scope VARCHAR(32) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    relative_path VARCHAR(500) NOT NULL,
    original_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT UNSIGNED NOT NULL,
    sha256 CHAR(64) NOT NULL,
    width INT UNSIGNED NULL,
    height INT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_assets_relative_path (relative_path),
    KEY idx_file_assets_scope_created_at (scope, created_at),
    KEY idx_file_assets_sha256 (sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
