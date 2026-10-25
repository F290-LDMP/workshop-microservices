package br.com.fatecararas.microservices.core.recommendation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import br.com.fatecararas.util.http.*;
import br.com.fatecararas.util.openapi.OpenApiConfiguration;
import br.com.fatecararas.util.messaging.RabbitConfiguration;
@SpringBootApplication
@Import({ServiceUtil.class, GlobalControllerExceptionHandler.class, OpenApiConfiguration.class, RabbitConfiguration.class})
public class RecommendationServiceApplication {
    public static void main(String[] args) { SpringApplication.run(RecommendationServiceApplication.class, args); }
}
