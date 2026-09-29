package br.com.fatecararas.api.core.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Recomendação")
public class Recommendation {
    @Schema(description = "Identificador do produto", example = "1", minimum = "1")
    private int productId;
    private int recommendationId;
    private String author;
    @Schema(description = "Nota de 0 a 5", example = "4", minimum = "0", maximum = "5")
    private int rate;
    private String content;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String serviceAddress;

    public Recommendation() { }

    public Recommendation(int productId, int recommendationId, String author, int rate, String content, String serviceAddress) {
        this.productId = productId;
        this.recommendationId = recommendationId;
        this.author = author;
        this.rate = rate;
        this.content = content;
        this.serviceAddress = serviceAddress;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public int getRecommendationId() { return recommendationId; }
    public void setRecommendationId(int recommendationId) { this.recommendationId = recommendationId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getRate() { return rate; }
    public void setRate(int rate) { this.rate = rate; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getServiceAddress() { return serviceAddress; }
    public void setServiceAddress(String serviceAddress) { this.serviceAddress = serviceAddress; }
}
