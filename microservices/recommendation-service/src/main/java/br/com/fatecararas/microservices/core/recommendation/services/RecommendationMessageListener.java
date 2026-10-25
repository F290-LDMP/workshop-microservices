package br.com.fatecararas.microservices.core.recommendation.services;
import br.com.fatecararas.api.core.recommendation.Recommendation;
import br.com.fatecararas.util.messaging.EventHandler;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
@Component
public class RecommendationMessageListener {
    private final RecommendationOperations operations;
    private final EventHandler handler;
    public RecommendationMessageListener(RecommendationOperations operations, EventHandler handler) { this.operations=operations; this.handler=handler; }
    @RabbitListener(queues="recommendation.commands")
    public void consume(Message message) {
        handler.handle(message, Recommendation.class, Recommendation::getProductId, operations::validate,
            body -> operations.getRecommendations(body.getProductId()).stream().filter(item -> item.getRecommendationId() == body.getRecommendationId()).findFirst().orElse(null), operations::createRecommendation, operations::deleteRecommendations);
    }
}
