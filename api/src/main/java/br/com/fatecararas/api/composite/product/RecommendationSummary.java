package br.com.fatecararas.api.composite.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter @AllArgsConstructor @NoArgsConstructor
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
}
