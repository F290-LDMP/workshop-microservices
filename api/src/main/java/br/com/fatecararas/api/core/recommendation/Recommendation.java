package br.com.fatecararas.api.core.recommendation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
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
}
