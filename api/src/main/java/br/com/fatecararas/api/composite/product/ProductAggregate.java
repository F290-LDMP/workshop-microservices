package br.com.fatecararas.api.composite.product;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Produto com recomendações e reviews")
public class ProductAggregate {
    @Schema(description = "Identificador do produto", example = "1", minimum = "1")
    private int productId;
    private String name;
    private int weight;
    private List<RecommendationSummary> recommendations;
    private List<ReviewSummary> reviews;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private ServiceAddresses serviceAddresses;

    public ProductAggregate() { }

    public ProductAggregate(int productId, String name, int weight, List<RecommendationSummary> recommendations, List<ReviewSummary> reviews, ServiceAddresses serviceAddresses) {
        this.productId = productId;
        this.name = name;
        this.weight = weight;
        this.recommendations = recommendations;
        this.reviews = reviews;
        this.serviceAddresses = serviceAddresses;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
    public List<RecommendationSummary> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationSummary> recommendations) { this.recommendations = recommendations; }
    public List<ReviewSummary> getReviews() { return reviews; }
    public void setReviews(List<ReviewSummary> reviews) { this.reviews = reviews; }
    public ServiceAddresses getServiceAddresses() { return serviceAddresses; }
    public void setServiceAddresses(ServiceAddresses serviceAddresses) { this.serviceAddresses = serviceAddresses; }
}
