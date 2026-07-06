-- Migration script for adding website settings fields to contact_info table
-- Execute this in your database to add the new columns

ALTER TABLE contact_info ADD COLUMN twitter_url VARCHAR(255);
ALTER TABLE contact_info ADD COLUMN site_name VARCHAR(255);
ALTER TABLE contact_info ADD COLUMN site_description LONGTEXT;
ALTER TABLE contact_info ADD COLUMN seo_keywords LONGTEXT;
ALTER TABLE contact_info ADD COLUMN commission_rate INT DEFAULT 15;

-- Add comments for clarity
ALTER TABLE contact_info MODIFY twitter_url VARCHAR(255) COMMENT 'Twitter social media link';
ALTER TABLE contact_info MODIFY site_name VARCHAR(255) COMMENT 'Website name/title';
ALTER TABLE contact_info MODIFY site_description LONGTEXT COMMENT 'Website SEO description';
ALTER TABLE contact_info MODIFY seo_keywords LONGTEXT COMMENT 'Website SEO keywords';
ALTER TABLE contact_info MODIFY commission_rate INT DEFAULT 15 COMMENT 'Global admin commission rate percentage';
