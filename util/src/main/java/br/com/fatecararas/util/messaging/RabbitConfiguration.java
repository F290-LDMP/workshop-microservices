package br.com.fatecararas.util.messaging;

import java.util.Map;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.retry.policy.SimpleRetryPolicy;

@Configuration
public class RabbitConfiguration {
    public static final String EXCHANGE = "products.commands";
    public static final String DLX = "products.dlx";

    @Bean
    public Declarables commandTopology() {
        var declarations = new java.util.ArrayList<Declarable>();
        declarations.add(new DirectExchange(EXCHANGE, true, false));
        declarations.add(new DirectExchange(DLX, true, false));
        for (String service : new String[]{"product", "review", "recommendation"}) {
            var queue = QueueBuilder.durable(service + ".commands")
                .deadLetterExchange(DLX).deadLetterRoutingKey(service)
                .withArgument("x-single-active-consumer", true).build();
            var dead = QueueBuilder.durable(service + ".dlq").build();
            declarations.add(queue);
            declarations.add(dead);
            declarations.add(new Binding(queue.getName(), Binding.DestinationType.QUEUE, EXCHANGE, service, null));
            declarations.add(new Binding(dead.getName(), Binding.DestinationType.QUEUE, DLX, service, null));
        }
        return new Declarables(declarations);
    }
    @Bean
    public Jackson2JsonMessageConverter rabbitJsonConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper, "br.com.fatecararas.api.event",
            "br.com.fatecararas.api.core.product", "br.com.fatecararas.api.core.review",
            "br.com.fatecararas.api.core.recommendation");
    }
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                               Jackson2JsonMessageConverter converter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        template.setReturnsCallback(returned -> { /* CorrelationData retains the returned message. */ });
        return template;
    }
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        var policy = new SimpleRetryPolicy(3,
            Map.of(org.springframework.amqp.AmqpRejectAndDontRequeueException.class, false), true, true);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless().retryPolicy(policy)
            .backOffOptions(1000, 2, 2000).recoverer(new RejectAndDontRequeueRecoverer()).build());
        return factory;
    }
    @Bean
    public EventPublisher eventPublisher(RabbitTemplate template,
            @org.springframework.beans.factory.annotation.Value("${app.messaging.confirm-timeout-ms:5000}") long timeout) {
        return new EventPublisher(template, timeout);
    }
    @Bean
    public EventHandler eventHandler(ObjectMapper mapper) { return new EventHandler(mapper); }
}
