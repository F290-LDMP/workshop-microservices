package br.com.fatecararas.api.core.recommendation;

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

@Tag(name = "recommendation", description = "Operações de recommendation")
public interface RecommendationService {
    @Operation(summary = "Criar recommendation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída"),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @PostMapping(value = "/recommendation", consumes = "application/json", produces = "application/json")
    Recommendation createRecommendation(@RequestBody Recommendation body);

    @Operation(summary = "Consultar recommendation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída"),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @GetMapping(value = "/recommendation", produces = "application/json")
    List<Recommendation> getRecommendations(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @RequestParam("productId") int productId);

    @Operation(summary = "Excluir recommendation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operação concluída", content = @Content),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @DeleteMapping(value = "/recommendation")
    void deleteRecommendations(@Parameter(description = "Identificador do produto", example = "1", required = true)
            @RequestParam("productId") int productId);
}
