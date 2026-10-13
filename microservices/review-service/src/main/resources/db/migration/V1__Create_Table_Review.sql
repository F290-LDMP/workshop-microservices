CREATE TABLE IF NOT EXISTS reviews (
    review_id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    author VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    content VARCHAR(2000) NOT NULL
);

