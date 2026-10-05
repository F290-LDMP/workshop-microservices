package br.com.fatecararas.api.composite.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Resumo de recomendação")
public class RecommendationSummary {
    @Schema(description = "Identificador da recomendação no serviço de recommendation", example = "1", minimum = "1")
    private int recommendationId;
    @Schema(description = "Autor da recomendação", example = "Ana")
    private String author;
    @Schema(description = "Nota de 0 a 5", example = "4", minimum = "0", maximum = "5")
    private int rate;
    @Schema(description = "Texto da recomendação", example = "Ótimo produto")
    private String content;

    public RecommendationSummary() { }

    public RecommendationSummary(int recommendationId, String author, int rate, String content) {
        this.recommendationId = recommendationId;
        this.author = author;
        this.rate = rate;
        this.content = content;
    }

}
