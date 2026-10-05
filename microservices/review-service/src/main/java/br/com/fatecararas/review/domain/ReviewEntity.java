package br.com.fatecararas.review.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "reviews", indexes = @Index(name = "idx_reviews_product", columnList = "productId"),
        uniqueConstraints = @UniqueConstraint(name = "uk_reviews_product_review", columnNames = {"productId", "reviewId"}))
public class ReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Integer version;

    private int productId;
    private int reviewId;
    private String author;
    private String subject;
    private String content;

    protected ReviewEntity() { }

    public ReviewEntity(int productId, int reviewId, String author, String subject, String content) {
        this.productId = productId;
        this.reviewId = reviewId;
        this.author = author;
        this.subject = subject;
        this.content = content;
    }

    public int getProductId() { return productId; }
    public int getReviewId() { return reviewId; }
    public String getAuthor() { return author; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
}
