ALTER TABLE news
    ADD COLUMN IF NOT EXISTS image_storage_path VARCHAR(512),
    ADD COLUMN IF NOT EXISTS image_original_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS image_content_type VARCHAR(128);
