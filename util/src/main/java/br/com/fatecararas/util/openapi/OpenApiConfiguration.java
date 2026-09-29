package br.com.fatecararas.util.openapi;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import br.com.fatecararas.util.http.HttpErrorInfo;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    @Bean
    public OpenAPI workshopOpenApi(
            @Value("${spring.application.name:workshop-microservices}") String applicationName) {
        Components components = new Components();
        ModelConverters.getInstance().read(HttpErrorInfo.class)
                .forEach(components::addSchemas);
        return new OpenAPI()
                .info(new Info().title(applicationName + " API")
                        .version("1.0.0")
                        .description("Contratos do workshop de microservices — FATEC"))
                .components(components);
    }
}
