-- MySQL: mantém AUTO_INCREMENT na chave técnica e preserva os IDs públicos.
ALTER TABLE reviews CHANGE COLUMN review_id id INT NOT NULL AUTO_INCREMENT;
ALTER TABLE reviews ADD COLUMN review_id INT;
UPDATE reviews SET review_id = id;
ALTER TABLE reviews MODIFY COLUMN review_id INT NOT NULL;
ALTER TABLE reviews ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE reviews ADD CONSTRAINT uq_reviews_product_review UNIQUE (product_id, review_id);
