package br.com.fatecararas.api.composite.product;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Schema(description = "Representação agregada do produto com recomendações, avaliações e endereços das instâncias que participaram da composição.", example = "{\"productId\":1,\"name\":\"produto 1\",\"weight\":100,\"recommendations\":[],\"reviews\":[],\"serviceAddresses\":{\"cmp\":\"http://localhost:7000\",\"pro\":\"http://localhost:7001\",\"rev\":\"http://localhost:7003\",\"rec\":\"http://localhost:7002\"}}")
public class ProductAggregate {
    @Schema(description = "Identificador do produto", example = "1", minimum = "1")
    private int productId;
    @Schema(description = "Nome apresentado para o produto", example = "produto 1")
    private String name;
    @Schema(description = "Peso do produto em gramas", example = "100", minimum = "0")
    private int weight;
    @Schema(description = "Recomendações associadas. Lista vazia quando não existem recomendações.")
    private List<RecommendationSummary> recommendations;
    @Schema(description = "Avaliações associadas. Lista vazia quando não existem avaliações.")
    private List<ReviewSummary> reviews;
    @Schema(description = "Endereços das instâncias participantes; preenchido pelo servidor.", accessMode = Schema.AccessMode.READ_ONLY)
    private ServiceAddresses serviceAddresses;

    public ProductAggregate(int productId, String name, int weight, List<RecommendationSummary> recommendations, List<ReviewSummary> reviews, ServiceAddresses serviceAddresses) {
        this.productId = productId;
        this.name = name;
        this.weight = weight;
        this.recommendations = recommendations;
        this.reviews = reviews;
        this.serviceAddresses = serviceAddresses;
    }
}
