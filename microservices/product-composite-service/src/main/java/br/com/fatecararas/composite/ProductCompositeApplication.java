package br.com.fatecararas.composite;

import br.com.fatecararas.util.http.GlobalControllerExceptionHandler;
import br.com.fatecararas.util.http.ServiceUtil;
import br.com.fatecararas.util.openapi.OpenApiConfiguration;
import br.com.fatecararas.composite.config.ServiceIds;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(ServiceIds.class)
@Import({ServiceUtil.class, GlobalControllerExceptionHandler.class, OpenApiConfiguration.class})
public class ProductCompositeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductCompositeApplication.class, args);
    }
}
