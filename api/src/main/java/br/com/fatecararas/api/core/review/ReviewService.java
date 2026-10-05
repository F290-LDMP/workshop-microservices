package br.com.fatecararas.api.core.review;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "review", description = "Operações de review")
public interface ReviewService {
    @Operation(summary = "Criar review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída"),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @PostMapping(value = "/review", consumes = "application/json", produces = "application/json")
    Review createReview(@RequestBody Review body);

    @Operation(summary = "Consultar review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída"),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @GetMapping(value = "/review", produces = "application/json")
    List<Review> getReviews(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @RequestParam("productId") int productId);

    @Operation(summary = "Excluir review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída", content = @Content),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @DeleteMapping(value = "/review")
    void deleteReviews(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @RequestParam("productId") int productId);
}
