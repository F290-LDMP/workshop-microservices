package br.com.fatecararas.api.composite.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Instâncias que atenderam à consulta")
public class ServiceAddresses {
    @Schema(description = "Instância do product-composite", example = "http://localhost:7000")
    private String cmp;
    @Schema(description = "Instância do product-service", example = "http://localhost:7001")
    private String pro;
    @Schema(description = "Instância do review-service", example = "http://localhost:7003")
    private String rev;
    @Schema(description = "Instância do recommendation-service", example = "http://localhost:7002")
    private String rec;

    public ServiceAddresses() { }

    public ServiceAddresses(String cmp, String pro, String rev, String rec) {
        this.cmp = cmp;
        this.pro = pro;
        this.rev = rev;
        this.rec = rec;
    }

}
