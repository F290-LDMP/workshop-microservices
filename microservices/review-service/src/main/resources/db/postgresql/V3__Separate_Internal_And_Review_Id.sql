-- Preserva os IDs e dados criados nas versões anteriores.
ALTER TABLE reviews RENAME COLUMN review_id TO id;
ALTER TABLE reviews ADD COLUMN review_id INT;
UPDATE reviews SET review_id = id;
ALTER TABLE reviews ALTER COLUMN review_id SET NOT NULL;
ALTER TABLE reviews ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE reviews ADD CONSTRAINT uq_reviews_product_review UNIQUE (product_id, review_id);
