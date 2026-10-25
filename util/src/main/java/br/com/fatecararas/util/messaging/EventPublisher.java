package br.com.fatecararas.util.messaging;

import br.com.fatecararas.api.event.Event;
import br.com.fatecararas.api.exceptions.MessagingUnavailableException;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class EventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(EventPublisher.class);
    private final RabbitTemplate template;
    private final long timeout;

    public EventPublisher(RabbitTemplate template, long timeout) {
        this.template = template;
        this.timeout = timeout;
    }

    public void publish(String destination, Event<?> event) {
        var correlation = new CorrelationData(event.eventId().toString());
        try {
            template.convertAndSend(
                    RabbitConfiguration.EXCHANGE, destination, event, message -> {
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setMessageId(event.eventId().toString());
                        message.getMessageProperties().setCorrelationId(event.correlationId().toString());
                        return message;
                    }, correlation
            );
            var confirm = correlation.getFuture().get(timeout, TimeUnit.MILLISECONDS);

            if (!confirm.isAck() || correlation.getReturned() != null)
                throw new IllegalStateException("Publication rejected or unroutable");
            LOG.info("Published eventId={} correlationId={} productId={} destination={}",
                    event.eventId(), event.correlationId(), event.productId(), destination);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new MessagingUnavailableException("Publication failed; some commands may already have been accepted", ex);
        }
    }
}
