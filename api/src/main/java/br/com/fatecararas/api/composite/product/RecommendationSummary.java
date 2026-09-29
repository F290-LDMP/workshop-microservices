package br.com.fatecararas.api.composite.product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de recomendação")
public class RecommendationSummary {
    private int recommendationId;
    private String author;
    @Schema(description = "Nota de 0 a 5", example = "4", minimum = "0", maximum = "5")
    private int rate;
    private String content;

    public RecommendationSummary() { }

    public RecommendationSummary(int recommendationId, String author, int rate, String content) {
        this.recommendationId = recommendationId;
        this.author = author;
        this.rate = rate;
        this.content = content;
    }

    public int getRecommendationId() { return recommendationId; }
    public void setRecommendationId(int recommendationId) { this.recommendationId = recommendationId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getRate() { return rate; }
    public void setRate(int rate) { this.rate = rate; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
