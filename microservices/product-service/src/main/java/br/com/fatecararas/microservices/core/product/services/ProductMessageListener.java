package br.com.fatecararas.microservices.core.product.services;
import br.com.fatecararas.api.core.product.Product;
import br.com.fatecararas.util.messaging.EventHandler;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
@Component
public class ProductMessageListener {
    private final ProductOperations operations;
    private final EventHandler handler;
    public ProductMessageListener(ProductOperations operations, EventHandler handler) { this.operations=operations; this.handler=handler; }
    @RabbitListener(queues="product.commands")
    public void consume(Message message) {
        handler.handle(message, Product.class, Product::getProductId, operations::validate,
            body -> {
                try { return operations.getProduct(body.getProductId()); }
                catch (br.com.fatecararas.api.exceptions.NotFoundException ex) { return null; }
            }, operations::createProduct, operations::deleteProduct);
    }
}
