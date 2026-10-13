package se.magnus.api.core.review.domain.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(
        name = "uq_reviews_product_review", columnNames = {"product_id", "review_id"}))
public class ReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Version
    private Integer version;
    @Column(name = "product_id", nullable = false)
    private int productId;
    @Column(name = "review_id", nullable = false)
    private int reviewId;
    @Column(nullable = false)
    private String author;
    @Column(nullable = false)
    private String subject;
    @Column(nullable = false, length = 2000)
    private String content;

    protected ReviewEntity() { }

    public ReviewEntity(int productId, int reviewId, String author, String subject, String content) {
        this.productId = productId;
        this.reviewId = reviewId;
        this.author = author;
        this.subject = subject;
        this.content = content;
    }

    public Integer getId() { return id; }
    public Integer getVersion() { return version; }
    public int getProductId() { return productId; }
    public int getReviewId() { return reviewId; }
    public String getAuthor() { return author; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
}
