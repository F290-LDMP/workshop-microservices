package br.com.fatecararas.api.composite.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Resumo de review")
public class ReviewSummary {
    @Schema(description = "Identificador da avaliação no serviço de review", example = "1", minimum = "1")
    private int reviewId;
    @Schema(description = "Autor da avaliação", example = "Bia")
    private String author;
    @Schema(description = "Assunto da avaliação", example = "Qualidade")
    private String subject;
    @Schema(description = "Texto da avaliação", example = "Atendeu às expectativas")
    private String content;

    public ReviewSummary() { }

    public ReviewSummary(int reviewId, String author, String subject, String content) {
        this.reviewId = reviewId;
        this.author = author;
        this.subject = subject;
        this.content = content;
    }

}
