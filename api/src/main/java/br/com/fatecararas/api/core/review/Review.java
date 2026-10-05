package br.com.fatecararas.api.core.review;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Review")
public class Review {
    @Schema(description = "Identificador do produto", example = "1", minimum = "1")
    private int productId;
    private int reviewId;
    private String author;
    private String subject;
    private String content;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String serviceAddress;
}
