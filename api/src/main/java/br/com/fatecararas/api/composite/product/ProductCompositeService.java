package br.com.fatecararas.api.composite.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Product Composite", description = "API agregadora de produto, recomendações e avaliações. Cada operação pode consultar ou coordenar os serviços de núcleo registrados no Eureka.")
public interface ProductCompositeService {
    @Operation(
        summary = "Criar produto agregado",
        description = "Cria o produto e, quando informados, suas recomendações e avaliações nos serviços de núcleo. A operação é uma cascata síncrona e não constitui uma transação distribuída.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Dados do produto e coleções opcionais de recomendações e avaliações.",
            required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ProductAggregate.class),
                examples = @ExampleObject(name = "Produto completo", value = "{\"productId\":1,\"name\":\"produto 1\",\"weight\":100,\"recommendations\":[{\"recommendationId\":1,\"author\":\"Ana\",\"rate\":5,\"content\":\"Ótimo produto\"}],\"reviews\":[{\"reviewId\":1,\"author\":\"Bia\",\"subject\":\"Qualidade\",\"content\":\"Atendeu às expectativas\"}]}"))))
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Produto e itens associados processados; resposta sem corpo", content = @Content),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = "/product-composite", consumes = "application/json")
    void createProduct(@RequestBody ProductAggregate body);

    @Operation(
        summary = "Consultar produto agregado",
        description = "Busca o produto, as recomendações e as avaliações em paralelo e reúne os resultados. Coleções sem itens são retornadas como arrays vazios. Os endereços indicam as instâncias participantes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Agregado encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ProductAggregate.class),
                examples = @ExampleObject(name = "Produto agregado", value = "{\"productId\":1,\"name\":\"produto 1\",\"weight\":100,\"recommendations\":[{\"recommendationId\":1,\"author\":\"Ana\",\"rate\":5,\"content\":\"Ótimo produto\"}],\"reviews\":[{\"reviewId\":1,\"author\":\"Bia\",\"subject\":\"Qualidade\",\"content\":\"Atendeu às expectativas\"}],\"serviceAddresses\":{\"cmp\":\"http://localhost:7000\",\"pro\":\"http://localhost:7001\",\"rev\":\"http://localhost:7003\",\"rec\":\"http://localhost:7002\"}}"))),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @GetMapping(value = "/product-composite/{productId}", produces = "application/json")
    ProductAggregate getProduct(@PathVariable @Parameter(description = "Identificador do produto", example = "1", required = true) int productId);

    @Operation(
        summary = "Excluir produto agregado",
        description = "Remove o produto e solicita a exclusão das recomendações e avaliações associadas. DELETE é idempotente nos serviços de núcleo; a cascata não é transacional.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Produto e itens associados processados; resposta sem corpo", content = @Content),
        @ApiResponse(responseCode = "400", description = "Requisição malformada",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo"))),
        @ApiResponse(responseCode = "422", description = "Dados inválidos para a operação",
            content = @Content(mediaType = "application/json",
                schema = @Schema(ref = "#/components/schemas/HttpErrorInfo")))
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping(value = "/product-composite/{productId}")
    void deleteProduct(@PathVariable @Parameter(description = "Identificador do produto", example = "1", required = true) int productId);
}
