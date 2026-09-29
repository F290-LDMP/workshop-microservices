package br.com.fatecararas.api.core.product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Produto")
public class Product {
    @Schema(description = "Identificador do produto", example = "1", minimum = "1")
    private int productId;
    private String name;
    private int weight;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String serviceAddress;

    public Product() { }

    public Product(int productId, String name, int weight, String serviceAddress) {
        this.productId = productId;
        this.name = name;
        this.weight = weight;
        this.serviceAddress = serviceAddress;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
    public String getServiceAddress() { return serviceAddress; }
    public void setServiceAddress(String serviceAddress) { this.serviceAddress = serviceAddress; }
}
