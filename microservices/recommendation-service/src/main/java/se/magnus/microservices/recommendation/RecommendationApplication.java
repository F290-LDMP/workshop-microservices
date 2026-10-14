package se.magnus.microservices.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import se.magnus.util.http.GlobalControllerExceptionHandler;
import se.magnus.util.http.OpenApiConfiguration;
import se.magnus.util.http.ServiceUtil;

@SpringBootApplication
@EnableDiscoveryClient
@Import({ServiceUtil.class, GlobalControllerExceptionHandler.class, OpenApiConfiguration.class})
public class RecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationApplication.class, args);
    }
}
