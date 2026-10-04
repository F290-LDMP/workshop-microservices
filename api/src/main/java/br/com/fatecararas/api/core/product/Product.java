package br.com.fatecararas.api.core.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Produto")
public class Product {
    @Schema(description = "Identificador do produto", example = "1", minimum = "1")
    private int productId;
    private String name;
    private int weight;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String serviceAddress;
}
