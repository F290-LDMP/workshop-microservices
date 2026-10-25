package br.com.fatecararas.microservices.core.review.persistence;
import jakarta.persistence.*;
@Entity
@Table(name="reviews", uniqueConstraints=@UniqueConstraint(columnNames={"productId", "reviewId"}))
public class ReviewEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Version private Integer version;
    private int productId;
    private int reviewId;
    private String author;
    private String subject;
    private String content;
    public ReviewEntity() { }
    public ReviewEntity(int productId, int reviewId, String author, String subject, String content) { this.productId=productId; this.reviewId=reviewId; this.author=author; this.subject=subject; this.content=content; }
    public int getProductId() { return productId; }
    public int getReviewId() { return reviewId; }
    public String getAuthor() { return author; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
}
