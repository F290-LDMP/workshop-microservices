package br.com.fatecararas.microservices.core.product;

import br.com.fatecararas.util.http.GlobalControllerExceptionHandler;
import br.com.fatecararas.util.http.ServiceUtil;
import br.com.fatecararas.util.openapi.OpenApiConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Import({ServiceUtil.class, GlobalControllerExceptionHandler.class, OpenApiConfiguration.class})
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
