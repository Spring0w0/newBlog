ALTER TABLE card_styles
    ADD COLUMN config JSON NULL AFTER enabled;

ALTER TABLE bloggers
    ADD COLUMN status VARCHAR(20) NULL AFTER stars;

ALTER TABLE projects
    ADD COLUMN project_year INT NULL AFTER name,
    ADD COLUMN tags JSON NULL AFTER image_url,
    ADD COLUMN github_url VARCHAR(1000) NULL AFTER tags,
    ADD COLUMN npm_url VARCHAR(1000) NULL AFTER github_url;

ALTER TABLE shares
    ADD COLUMN tags JSON NULL AFTER description,
    ADD COLUMN stars INT NOT NULL DEFAULT 3 AFTER tags;

ALTER TABLE pictures
    ADD COLUMN uploaded_at DATETIME(3) NULL AFTER sort_order;
