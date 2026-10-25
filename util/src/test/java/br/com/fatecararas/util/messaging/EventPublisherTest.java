package br.com.fatecararas.util.messaging;

import br.com.fatecararas.api.event.Event;
import br.com.fatecararas.api.exceptions.MessagingUnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventPublisherTest {
    final RabbitTemplate template = mock(RabbitTemplate.class);
    void confirm(boolean ack, boolean returned) {
        doAnswer(call -> {
            CorrelationData correlation = call.getArgument(4);
            if (returned) correlation.setReturned(new ReturnedMessage(new Message(new byte[0], new MessageProperties()), 312, "NO_ROUTE", "products.commands", "product"));
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, "test"));
            return null;
        }).when(template).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class), any(CorrelationData.class));
    }
    @Test void acceptsConfirmedPublication() {
        confirm(true, false);
        assertDoesNotThrow(() -> new EventPublisher(template, 10).publish("product", Event.delete(UUID.randomUUID(), 1)));
    }
    @Test void rejectsNack() {
        confirm(false, false);
        assertThrows(MessagingUnavailableException.class, () -> new EventPublisher(template, 10).publish("product", Event.delete(UUID.randomUUID(), 1)));
    }
    @Test void rejectsReturnedMessageEvenWithAck() {
        confirm(true, true);
        assertThrows(MessagingUnavailableException.class, () -> new EventPublisher(template, 10).publish("product", Event.delete(UUID.randomUUID(), 1)));
    }
    @Test void rejectsMissingConfirmation() {
        assertThrows(MessagingUnavailableException.class, () -> new EventPublisher(template, 1).publish("product", Event.delete(UUID.randomUUID(), 1)));
    }
    @Test void rejectsUnavailableBroker() {
        doThrow(new org.springframework.amqp.AmqpConnectException(new java.net.ConnectException("offline")))
            .when(template).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class), any(CorrelationData.class));
        assertThrows(MessagingUnavailableException.class, () -> new EventPublisher(template, 10).publish("product", Event.delete(UUID.randomUUID(), 1)));
    }
}
