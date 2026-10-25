package br.com.fatecararas.microservices.core.review.services;
import br.com.fatecararas.api.core.review.Review;
import br.com.fatecararas.util.messaging.EventHandler;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
@Component
public class ReviewMessageListener {
    private final ReviewOperations operations;
    private final EventHandler handler;
    public ReviewMessageListener(ReviewOperations operations, EventHandler handler) { this.operations=operations; this.handler=handler; }
    @RabbitListener(queues="review.commands")
    public void consume(Message message) {
        handler.handle(message, Review.class, Review::getProductId, operations::validate,
            body -> operations.getReviews(body.getProductId()).stream().filter(item -> item.getReviewId() == body.getReviewId()).findFirst().orElse(null), operations::createReview, operations::deleteReviews);
    }
}
