package se.magnus.microservices.recommendation;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "recommendations")
@CompoundIndex(name = "prod-rec-id", def = "{'productId': 1, 'recommendationId': 1}", unique = true)
public class RecommendationEntity {

    @Id
    private String id;
    @Version
    private Integer version;
    private int productId;
    private int recommendationId;
    private String author;
    private int rating;
    private String content;

    public RecommendationEntity() {
    }

    public RecommendationEntity(int productId, int recommendationId, String author, int rating, String content) {
        this.productId = productId;
        this.recommendationId = recommendationId;
        this.author = author;
        this.rating = rating;
        this.content = content;
    }

    public String getId() { return id; }
    public Integer getVersion() { return version; }
    public int getProductId() { return productId; }
    public int getRecommendationId() { return recommendationId; }
    public String getAuthor() { return author; }
    public int getRating() { return rating; }
    public String getContent() { return content; }
}
